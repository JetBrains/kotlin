/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.impl

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Assertions
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.LanguageVersionSettingsBuilder
import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.ModuleStructureDirectives
import org.jetbrains.kotlin.test.directives.TargetPlatformEnum
import org.jetbrains.kotlin.test.directives.model.ComposedRegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.Directive
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl.Companion.toArtifactKind
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
@OptIn(TestInfrastructureInternals::class)
class ModuleStructureExtractorImpl(
    testServices: TestServices,
    additionalSourceProviders: List<AdditionalSourceProvider>,
    moduleStructureTransformers: List<ModuleStructureTransformer>,
    private val environmentConfigurators: List<AbstractEnvironmentConfigurator>
) : ModuleStructureExtractor(testServices, additionalSourceProviders, moduleStructureTransformers) {
    companion object {
        private val allowedExtensionsForFiles = listOf(".kt", ".kts", ".java", ".js", ".mjs", ".config", ".xml")

        /*
         * ([^()\n]+) module name
         * \((.*?)\) module dependencies
         * (\((.*?)\)(\((.*?)\))?)? module friendDependencies and dependsOnDependencies
         */
        private val moduleDirectiveRegex = """([^()\n]+)(\((.*?)\)(\((.*?)\)(\((.*?)\))?)?)?""".toRegex()
    }

    override fun splitTestDataByModules(
        testDataFileName: String,
        directivesContainer: DirectivesContainer,
    ): TestModuleStructure {
        val testDataFile = File(testDataFileName)
        val extractor = ModuleStructureExtractorWorker(listOf(testDataFile), directivesContainer)
        var result = extractor.splitTestDataByModules()
        for (transformer in moduleStructureTransformers) {
            result = try {
                transformer.transformModuleStructure(result, testServices.defaultsProvider)
            } catch (e: Throwable) {
                throw ExceptionFromModuleStructureTransformer(e, result)
            }
        }
        return result
    }

    private inner class ModuleStructureExtractorWorker(
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

        private var currentModuleName: String? = null
        private var currentModuleTargetPlatform: TargetPlatform? = null
        private var currentModuleFrontendKind: FrontendKind<*>? = null
        private var currentModuleTargetBackend: TargetBackend? = null
        private var currentModuleLanguageVersionSettingsBuilder: LanguageVersionSettingsBuilder = initLanguageSettingsBuilder()
        private var dependenciesOfCurrentModule = mutableListOf<DependencyDescription>()
        private var filesOfCurrentModule = mutableListOf<TestFile>()

        private var currentFileName: String? = null
        private var firstFileInModule: Boolean = true
        private var linesOfCurrentFile = mutableListOf<String>()
        private var endLineNumberOfLastFile = -1

        private var allowFilesWithSameNames = false

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
            finishModule(lineNumber = -1)
            val sortedModules = sortModules(modules)
            checkCycles(modules)
            return TestModuleStructureImpl(sortedModules, testDataFiles)
        }

        private fun sortModules(modules: List<TestModule>): List<TestModule> {
            val moduleByName = modules.groupBy { it.name }.mapValues { (name, modules) ->
                modules.singleOrNull() ?: error("Duplicated modules with name $name")
            }
            return DFS.topologicalOrder(modules) { module ->
                module.allDependencies.map {
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
                for (dependency in module.allDependencies) {
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
                        finishModule(lineNumber)
                    } else {
                        finishGlobalDirectives()
                    }
                    val (moduleName, dependencies, friends, dependsOn) = splitRawModuleStringToNameAndDependencies(
                        values.joinToString(separator = " ")
                    )
                    currentModuleName = moduleName
                    val kind = defaultsProvider.defaultDependencyKind
                    dependencies.mapTo(dependenciesOfCurrentModule) { name ->
                        DependencyDescription(name, kind, DependencyRelation.RegularDependency)
                    }
                    friends.mapTo(dependenciesOfCurrentModule) { name ->
                        DependencyDescription(name, kind, DependencyRelation.FriendDependency)
                    }
                    dependsOn.mapTo(dependenciesOfCurrentModule) { name ->
                        DependencyDescription(name, DependencyKind.Source, DependencyRelation.DependsOnDependency)
                    }
                }
                ModuleStructureDirectives.DEPENDENCY,
                ModuleStructureDirectives.DEPENDS_ON -> {
                    val name = values.first() as String
                    val kind = values.getOrNull(1)?.let { valueOfOrNull(it as String) } ?: defaultsProvider.defaultDependencyKind
                    val relation = when (directive) {
                        ModuleStructureDirectives.DEPENDENCY -> DependencyRelation.RegularDependency
                        ModuleStructureDirectives.DEPENDS_ON -> DependencyRelation.DependsOnDependency
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
                        finishFile(lineNumber)
                    } else {
                        resetFileCaches()
                    }
                    currentFileName = (values.first() as String).also(::validateFileName)
                }
                ModuleStructureDirectives.ALLOW_FILES_WITH_SAME_NAMES -> {
                    allowFilesWithSameNames = true
                }
                ModuleStructureDirectives.TARGET_PLATFORM -> {
                    if (currentModuleTargetPlatform != null) {
                        assertions.fail { "Target platform already specified twice for module $currentModuleName" }
                    }
                    val platforms = values.map { (it as TargetPlatformEnum).targetPlatform }
                    currentModuleTargetPlatform = when (platforms.size) {
                        0 -> assertions.fail { "Target platform specified incorrectly\nUsage: ${directive.description}" }
                        1 -> platforms.single()
                        else -> {
                            if (TargetPlatformEnum.Common in values) {
                                assertions.fail { "You can't specify `Common` platform in combination with others" }
                            }
                            TargetPlatform(platforms.flatMapTo(mutableSetOf()) { it.componentPlatforms })
                        }
                    }
                }
                ModuleStructureDirectives.JVM_TARGET -> {
                    if (!defaultsProvider.defaultPlatform.isJvm()) return false
                    if (currentModuleTargetPlatform != null) {
                        assertions.fail { "Target platform already specified twice for module $currentModuleName" }
                    }
                    currentModuleTargetPlatform = if (values.size != 1) {
                        assertions.fail { "JVM target should be single" }
                    } else {
                        when (values.single()) {
                            "1.6" -> JvmPlatforms.jvm6
                            "1.8" -> JvmPlatforms.jvm8
                            "11" -> JvmPlatforms.jvm11
                            "17" -> JvmPlatforms.jvm17
                            else -> assertions.fail { "Incorrect value for JVM target" }
                        }
                    }
                    return false // Workaround for FE and FIR
                }
                else -> return false
            }

            return true
        }

        private fun splitRawModuleStringToNameAndDependencies(moduleDirectiveString: String): ModuleNameAndDependencies {
            val matchResult = moduleDirectiveRegex.matchEntire(moduleDirectiveString)
                ?: error("\"$moduleDirectiveString\" doesn't matches with pattern \"moduleName(dep1, dep2)\"")
            val (name, _, dependencies, _, friends, _, dependsOn) = matchResult.destructured
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
            val friendsNames = friends.takeIf { it.isNotBlank() }?.split(" ") ?: emptyList()
            val dependsOnNames = dependsOn.takeIf { it.isNotBlank() }?.split(" ") ?: emptyList()

            val intersection = buildSet {
                addAll(dependenciesNames intersect friendsNames)
                addAll(dependenciesNames intersect dependsOnNames)
                addAll(friendsNames intersect dependsOnNames)
            }
            require(intersection.isEmpty()) {
                val m = if (intersection.size == 1) "module" else "modules"
                val names = if (intersection.size == 1) "`${intersection.first()}`" else intersection.joinToArrayString()
                """Module `$name` depends on $m $names with different kinds simultaneously"""
            }

            return ModuleNameAndDependencies(
                name,
                dependenciesNames,
                friendsNames,
                dependsOnNames,
            )
        }

        private fun finishGlobalDirectives() {
            globalDirectives = directivesBuilder.build().onEach { it.checkDirectiveApplicability(contextIsGlobal = true) }
            resetModuleCaches()
            resetFileCaches()
        }

        private fun Directive.checkDirectiveApplicability(
            contextIsGlobal: Boolean = false,
            contextIsModule: Boolean = false,
            contextIsFile: Boolean = false
        ) {
            when {
                applicability.forGlobal && contextIsGlobal -> return
                applicability.forModule && contextIsModule -> return
                applicability.forFile && contextIsFile -> return
            }
            val context = buildList {
                if (contextIsGlobal) add("Global")
                if (contextIsModule) add("Module")
                if (contextIsFile) add("File")
            }.joinToString("|")
            error("Directive $this has $applicability applicability but it declared in $context")
        }

        private fun finishModule(lineNumber: Int) {
            finishFile(lineNumber)
            val isImplicitModule = currentModuleName == null
            val moduleDirectives = moduleDirectivesBuilder.build() + testServices.defaultDirectives + globalDirectives
            moduleDirectives.forEach { it.checkDirectiveApplicability(contextIsGlobal = isImplicitModule, contextIsModule = true) }

            val targetBackend = currentModuleTargetBackend ?: defaultsProvider.defaultTargetBackend
            val frontendKind = currentModuleFrontendKind ?: defaultsProvider.defaultFrontend

            currentModuleLanguageVersionSettingsBuilder.configureUsingDirectives(
                moduleDirectives, environmentConfigurators, targetBackend, useK2 = frontendKind == FrontendKinds.FIR
            )
            val moduleName = currentModuleName
                ?: testServices.defaultDirectives[ModuleStructureDirectives.MODULE].firstOrNull()
                ?: DEFAULT_MODULE_NAME
            val targetPlatform = currentModuleTargetPlatform ?: parseModulePlatformByName(moduleName) ?: defaultsProvider.defaultPlatform
            val testModule = TestModule(
                name = moduleName,
                targetPlatform = targetPlatform,
                targetBackend = targetBackend,
                frontendKind = currentModuleFrontendKind ?: defaultsProvider.defaultFrontend,
                backendKind = BackendKinds.fromTargetBackend(targetBackend),
                binaryKind = defaultsProvider.defaultArtifactKind ?: targetPlatform.toArtifactKind(frontendKind),
                files = filesOfCurrentModule,
                allDependencies = dependenciesOfCurrentModule,
                directives = moduleDirectives,
                languageVersionSettings = currentModuleLanguageVersionSettingsBuilder.build()
            )
            if (testModule.frontendKind != FrontendKinds.FIR ||
                !testModule.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) ||
                modules.isEmpty()
            ) {
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
            }
            modules += testModule
            firstFileInModule = true
            resetModuleCaches()
        }

        private fun parseModulePlatformByName(moduleName: String): TargetPlatform? {
            val nameSuffix = moduleName.substringAfterLast("-", "").uppercase()
            return when {
                nameSuffix == "COMMON" -> CommonPlatforms.defaultCommonPlatform
                nameSuffix == "JVM" -> JvmPlatforms.unspecifiedJvmPlatform // TODO(dsavvinov): determine JvmTarget precisely
                nameSuffix == "JS" -> JsPlatforms.defaultJsPlatform
                nameSuffix == "NATIVE" -> NativePlatforms.unspecifiedNativePlatform
                nameSuffix.isEmpty() -> null // TODO(dsavvinov): this leads to 'null'-platform in ModuleDescriptor
                else -> throw IllegalStateException("Can't determine platform by name $nameSuffix")
            }
        }

        private fun finishFile(lineNumber: Int) {
            val actualDefaultFileName = if (currentModuleName == null) {
                defaultFileName
            } else {
                "module_${currentModuleName}_$defaultFileName"
            }
            val filename = currentFileName ?: actualDefaultFileName
            if (!allowFilesWithSameNames && filesOfCurrentModule.any { it.name == filename }) {
                error("File with name \"$filename\" already defined in module ${currentModuleName ?: actualDefaultFileName}")
            }
            val directives = fileDirectivesBuilder?.build()?.onEach { it.checkDirectiveApplicability(contextIsFile = true) }
            val fileContent = buildString {
                for (i in 0 until endLineNumberOfLastFile) {
                    appendLine()
                }
                appendLine(linesOfCurrentFile.joinToString("\n"))
            }
            filesOfCurrentModule.add(
                TestFile(
                    relativePath = filename,
                    originalContent = fileContent,
                    originalFile = currentTestDataFile,
                    startLineNumberInOriginalFile = endLineNumberOfLastFile,
                    isAdditional = false,
                    directives = directives ?: RegisteredDirectives.Empty
                )
            )
            firstFileInModule = false
            endLineNumberOfLastFile = lineNumber - 1
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
            resetDirectivesBuilder()
            moduleDirectivesBuilder = directivesBuilder
        }

        private fun resetDirectivesBuilder() {
            directivesBuilder = RegisteredDirectivesParser(directivesContainer, assertions)
        }

        private fun resetFileCaches() {
            if (!firstFileInModule) {
                linesOfCurrentFile = mutableListOf()
            }
            if (firstFileInModule) {
                moduleDirectivesBuilder = directivesBuilder
            }
            currentFileName = null
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

    private data class ModuleNameAndDependencies(
        val name: String,
        val dependencies: List<String>,
        val friends: List<String>,
        val dependsOn: List<String>
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

