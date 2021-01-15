/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.impl

import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.builders.LanguageVersionSettingsBuilder
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives
import org.jetbrains.kotlin.test.directives.model.ComposedRegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.util.joinToArrayString
import org.jetbrains.kotlin.utils.DFS
import java.io.File

/*
 * Rules of directives resolving:
 * - If no `MODULE` or `FILE` was declared in test then all directives belongs to module
 * - If `FILE` is declared, then all directives after it will belong to
 *   file until next `FILE` or `MODULE` directive will be declared
 * - All directives between `MODULE` and `FILE` directives belongs to module
 * - All directives before first `MODULE` are global and belongs to each declared module
 */
class ModuleStructureExtractorImpl(
    testServices: TestServices,
    additionalSourceProviders: List<AdditionalSourceProvider>,
    private val environmentConfigurators: List<EnvironmentConfigurator>
) : ModuleStructureExtractor(testServices, additionalSourceProviders) {
    companion object {
        private val allowedExtensionsForFiles = listOf(".kt", ".kts", ".java")

        /*
         * ([\w-]+) module name
         * \((.*?)\) module dependencies
         * (\((.*?)\))? module friends
         */
        private val moduleDirectiveRegex = """([\w-]+)(\((.*?)\)(\((.*?)\))?)?""".toRegex()
    }

    override fun splitTestDataByModules(
        testDataFileName: String,
        directivesContainer: DirectivesContainer,
    ): TestModuleStructure {
        val testDataFile = File(testDataFileName)
        val extractor = ModuleStructureExtractorWorker(listOf(testDataFile), directivesContainer)
        return extractor.splitTestDataByModules()
    }

    private inner class ModuleStructureExtractorWorker constructor(
        private val testDataFiles: List<File>,
        private val directivesContainer: DirectivesContainer,
    ) {
        private val assertions: Assertions
            get() = testServices.assertions

        private val defaultsProvider: DefaultsProvider
            get() = testServices.defaultsProvider

        private lateinit var currentTestDataFile: File

        private val defaultFileName: String
            get() = currentTestDataFile.name

        private val defaultModuleName: String
            get() = "main"

        private var currentModuleName: String? = null
        private var currentModuleTargetPlatform: TargetPlatform? = null
        private var currentModuleFrontendKind: FrontendKind<*>? = null
        private var currentModuleTargetBackend: TargetBackend? = null
        private var currentModuleLanguageVersionSettingsBuilder: LanguageVersionSettingsBuilder = initLanguageSettingsBuilder()
        private var dependenciesOfCurrentModule = mutableListOf<DependencyDescription>()
        private var friendsOfCurrentModule = mutableListOf<DependencyDescription>()
        private var filesOfCurrentModule = mutableListOf<TestFile>()

        private var currentFileName: String? = null
        private var firstFileInModule: Boolean = true
        private var linesOfCurrentFile = mutableListOf<String>()
        private var startLineNumberOfCurrentFile = 0

        private var directivesBuilder = RegisteredDirectivesParser(directivesContainer, assertions)
        private var moduleDirectivesBuilder: RegisteredDirectivesParser = directivesBuilder
        private var fileDirectivesBuilder: RegisteredDirectivesParser? = null

        private var globalDirectives: RegisteredDirectives? = null

        private val modules = mutableListOf<TestModule>()

        private val moduleStructureDirectiveBuilder = RegisteredDirectivesParser(ModuleStructureDirectives, assertions)

        fun splitTestDataByModules(): TestModuleStructure {
            for (testDataFile in testDataFiles) {
                currentTestDataFile = testDataFile
                val lines = testDataFile.readLines()
                lines.forEachIndexed { lineNumber, line ->
                    val rawDirective = RegisteredDirectivesParser.parseDirective(line)
                    if (tryParseStructureDirective(rawDirective, lineNumber + 1)) {
                        linesOfCurrentFile.add(line)
                        return@forEachIndexed
                    }
                    tryParseRegularDirective(rawDirective)
                    linesOfCurrentFile.add(line)
                }
            }
            finishModule()
            val sortedModules = sortModules(modules)
            checkCycles(modules)
            return TestModuleStructureImpl(sortedModules, testDataFiles)
        }

        private fun sortModules(modules: List<TestModule>): List<TestModule> {
            val moduleByName = modules.groupBy { it.name }.mapValues { (name, modules) ->
                modules.singleOrNull() ?: error("Duplicated modules with name $name")
            }
            return DFS.topologicalOrder(modules) { module ->
                module.dependencies.map {
                    val moduleName = it.moduleName
                    moduleByName[moduleName] ?: error("Module \"$moduleName\" not found while observing dependencies of \"${module.name}\"")
                }
            }.asReversed()
        }

        private fun checkCycles(modules: List<TestModule>) {
            val visited = mutableSetOf<String>()
            for (module in modules) {
                val moduleName = module.name
                visited.add(moduleName)
                for (dependency in module.dependencies) {
                    val dependencyName = dependency.moduleName
                    if (dependencyName == moduleName) {
                        error("Module $moduleName has dependency to itself")
                    }
                    if (dependencyName !in visited) {
                        error("There is cycle in modules dependencies. See modules: $dependencyName, $moduleName")
                    }
                }
            }
        }

        /*
         * returns [true] means that passed directive was module directive and line is processed
         */
        private fun tryParseStructureDirective(rawDirective: RegisteredDirectivesParser.RawDirective?, lineNumber: Int): Boolean {
            if (rawDirective == null) return false
            val (directive, values) = moduleStructureDirectiveBuilder.convertToRegisteredDirective(rawDirective) ?: return false
            when (directive) {
                ModuleStructureDirectives.MODULE -> {
                    /*
                     * There was previous module, so we should save it
                     */
                    if (currentModuleName != null) {
                        finishModule()
                    } else {
                        finishGlobalDirectives()
                    }
                    val (moduleName, dependencies, friends) = splitRawModuleStringToNameAndDependencies(values.joinToString(separator = " "))
                    currentModuleName = moduleName
                    dependencies.mapTo(dependenciesOfCurrentModule) { name ->
                        val dependentModule = modules.firstOrNull { it.name == name }
                        if (dependentModule?.targetPlatform.isCommon()) {
                            return@mapTo DependencyDescription(name, DependencyKind.Source, DependencyRelation.DependsOn)
                        }
                        val kind = defaultsProvider.defaultDependencyKind
                        DependencyDescription(name, kind, DependencyRelation.Dependency)
                    }
                    friends.mapTo(friendsOfCurrentModule) { name ->
                        val kind = defaultsProvider.defaultDependencyKind
                        DependencyDescription(name, kind, DependencyRelation.Dependency)
                    }
                }
                ModuleStructureDirectives.DEPENDENCY,
                ModuleStructureDirectives.DEPENDS_ON -> {
                    val name = values.first() as String
                    val kind = values.getOrNull(1)?.let { valueOfOrNull(it as String) } ?: defaultsProvider.defaultDependencyKind
                    val relation = when (directive) {
                        ModuleStructureDirectives.DEPENDENCY -> DependencyRelation.Dependency
                        ModuleStructureDirectives.DEPENDS_ON -> DependencyRelation.DependsOn
                        else -> error("Should not be here")
                    }
                    dependenciesOfCurrentModule.add(DependencyDescription(name, kind, relation))
                }
                ModuleStructureDirectives.TARGET_FRONTEND -> {
                    val name = values.singleOrNull() as? String? ?: assertions.fail {
                        "Target frontend specified incorrectly\nUsage: ${directive.description}"
                    }
                    currentModuleFrontendKind = FrontendKinds.fromString(name) ?: assertions.fail {
                        "Unknown frontend: $name"
                    }
                }
                ModuleStructureDirectives.TARGET_BACKEND_KIND -> {
                    currentModuleTargetBackend = values.single() as TargetBackend
                }
                ModuleStructureDirectives.FILE -> {
                    if (currentFileName != null) {
                        finishFile()
                    } else {
                        resetFileCaches()
                    }
                    currentFileName = (values.first() as String).also(::validateFileName)
                    startLineNumberOfCurrentFile = lineNumber
                }
                else -> return false
            }

            return true
        }

        private fun splitRawModuleStringToNameAndDependencies(moduleDirectiveString: String): ModuleNameAndDependeciens {
            val matchResult = moduleDirectiveRegex.matchEntire(moduleDirectiveString)
                ?: error("\"$moduleDirectiveString\" doesn't matches with pattern \"moduleName(dep1, dep2)\"")
            val (name, _, dependencies, _, friends) = matchResult.destructured
            var dependenciesNames = dependencies.takeIf { it.isNotBlank() }?.split(" ") ?: emptyList()
            globalDirectives?.let { directives ->
                /*
                 * In old tests coroutine helpers was added as separate module named `support`
                 *   instead of additional files for current module. So to safe compatibility with
                 *   old testdata we need to filter this dependency
                 */
                if (AdditionalFilesDirectives.WITH_COROUTINES in directives) {
                    dependenciesNames = dependenciesNames.filter { it != "support" }
                }
            }
            return ModuleNameAndDependeciens(
                name,
                dependenciesNames,
                friends.takeIf { it.isNotBlank() }?.split(" ") ?: emptyList(),
            )
        }

        private fun finishGlobalDirectives() {
            globalDirectives = directivesBuilder.build()
            resetModuleCaches()
            resetFileCaches()
        }

        private fun finishModule() {
            finishFile()
            val moduleDirectives = moduleDirectivesBuilder.build() + testServices.defaultDirectives + globalDirectives
            currentModuleLanguageVersionSettingsBuilder.configureUsingDirectives(moduleDirectives, environmentConfigurators)
            val moduleName = currentModuleName ?: defaultModuleName
            val testModule = TestModule(
                name = moduleName,
                targetPlatform = currentModuleTargetPlatform ?: parseModulePlatformByName(moduleName) ?: defaultsProvider.defaultPlatform,
                targetBackend = currentModuleTargetBackend ?: defaultsProvider.defaultTargetBackend,
                frontendKind = currentModuleFrontendKind ?: defaultsProvider.defaultFrontend,
                files = filesOfCurrentModule,
                dependencies = dependenciesOfCurrentModule,
                friends = friendsOfCurrentModule,
                directives = moduleDirectives,
                languageVersionSettings = currentModuleLanguageVersionSettingsBuilder.build()
            )
            modules += testModule
            additionalSourceProviders.flatMapTo(filesOfCurrentModule) { additionalSourceProvider ->
                additionalSourceProvider.produceAdditionalFiles(
                    globalDirectives ?: RegisteredDirectives.Empty,
                    testModule
                ).also { additionalFiles ->
                    require(additionalFiles.all { it.isAdditional }) {
                        "Files produced by ${additionalSourceProvider::class.qualifiedName} should have flag `isAdditional = true`"
                    }
                }
            }
            firstFileInModule = true
            resetModuleCaches()
        }

        private fun parseModulePlatformByName(moduleName: String): TargetPlatform? {
            val nameSuffix = moduleName.substringAfterLast("-", "").toUpperCase()
            return when {
                nameSuffix == "COMMON" -> CommonPlatforms.defaultCommonPlatform
                nameSuffix == "JVM" -> JvmPlatforms.unspecifiedJvmPlatform // TODO(dsavvinov): determine JvmTarget precisely
                nameSuffix == "JS" -> JsPlatforms.defaultJsPlatform
                nameSuffix == "NATIVE" -> NativePlatforms.unspecifiedNativePlatform
                nameSuffix.isEmpty() -> null // TODO(dsavvinov): this leads to 'null'-platform in ModuleDescriptor
                else -> throw IllegalStateException("Can't determine platform by name $nameSuffix")
            }
        }

        private fun finishFile() {
            val actualDefaultFileName = if (currentModuleName == null) {
                defaultFileName
            } else {
                "module_${currentModuleName}_$defaultFileName"
            }
            val filename = currentFileName ?: actualDefaultFileName
            if (filesOfCurrentModule.any { it.name == filename }) {
                error("File with name \"$filename\" already defined in module ${currentModuleName ?: actualDefaultFileName}")
            }
            filesOfCurrentModule.add(
                TestFile(
                    relativePath = filename,
                    originalContent = linesOfCurrentFile.joinToString(separator = "\n", postfix = "\n"),
                    originalFile = currentTestDataFile,
                    startLineNumberInOriginalFile = startLineNumberOfCurrentFile,
                    isAdditional = false,
                    directives = fileDirectivesBuilder?.build() ?: RegisteredDirectives.Empty
                )
            )
            firstFileInModule = false
            resetFileCaches()
        }

        private fun resetModuleCaches() {
            firstFileInModule = true
            currentModuleName = null
            currentModuleTargetPlatform = null
            currentModuleTargetBackend = null
            currentModuleFrontendKind = null
            currentModuleLanguageVersionSettingsBuilder = initLanguageSettingsBuilder()
            filesOfCurrentModule = mutableListOf()
            dependenciesOfCurrentModule = mutableListOf()
            friendsOfCurrentModule = mutableListOf()
            resetDirectivesBuilder()
            moduleDirectivesBuilder = directivesBuilder
        }

        private fun resetDirectivesBuilder() {
            directivesBuilder = RegisteredDirectivesParser(directivesContainer, assertions)
        }

        private fun resetFileCaches() {
            if (!firstFileInModule) {
                linesOfCurrentFile = mutableListOf()
                moduleDirectivesBuilder = directivesBuilder
            }
            currentFileName = null
            startLineNumberOfCurrentFile = 0
            resetDirectivesBuilder()
            fileDirectivesBuilder = directivesBuilder
        }

        private fun tryParseRegularDirective(rawDirective: RegisteredDirectivesParser.RawDirective?) {
            if (rawDirective == null) return
            val parsedDirective = directivesBuilder.convertToRegisteredDirective(rawDirective) ?: return
            directivesBuilder.addParsedDirective(parsedDirective)
        }

        private fun validateFileName(fileName: String) {
            if (!allowedExtensionsForFiles.any { fileName.endsWith(it) }) {
                assertions.fail {
                    "Filename $fileName is not valid. Allowed extensions: ${allowedExtensionsForFiles.joinToArrayString()}"
                }
            }
        }

        private fun initLanguageSettingsBuilder(): LanguageVersionSettingsBuilder {
            return defaultsProvider.newLanguageSettingsBuilder()
        }
    }

    private data class ModuleNameAndDependeciens(
        val name: String,
        val dependencies: List<String>,
        val friends: List<String>
    )
}

private operator fun RegisteredDirectives.plus(other: RegisteredDirectives?): RegisteredDirectives {
    return when {
        other == null -> this
        other.isEmpty() -> this
        this.isEmpty() -> other
        else -> ComposedRegisteredDirectives(this, other)
    }
}

inline fun <reified T : Enum<T>> valueOfOrNull(value: String): T? {
    for (enumValue in enumValues<T>()) {
        if (enumValue.name == value) {
            return enumValue
        }
    }
    return null
}

