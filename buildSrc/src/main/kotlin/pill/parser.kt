/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.tasks.*
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.HasConvention
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.SingleParentCopySpec
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.pill.POrderRoot.*
import org.jetbrains.kotlin.pill.PSourceRoot.*
import org.jetbrains.kotlin.pill.PillExtension.*
import java.io.File
import java.util.LinkedList

data class PProject(
    val name: String,
    val rootDirectory: File,
    val modules: List<PModule>,
    val libraries: List<PLibrary>
)

data class PModule(
    val name: String,
    val bundleName: String,
    val rootDirectory: File,
    val moduleFile: File,
    val contentRoots: List<PContentRoot>,
    val orderRoots: List<POrderRoot>,
    val moduleForProductionSources: PModule? = null,
    val group: String? = null
)

data class PContentRoot(
    val path: File,
    val forTests: Boolean,
    val sourceRoots: List<PSourceRoot>,
    val excludedDirectories: List<File>
)

data class PSourceRoot(
    val path: File,
    val kind: Kind,
    val kotlinOptions: PSourceRootKotlinOptions?
) {
    enum class Kind { PRODUCTION, TEST, RESOURCES, TEST_RESOURCES }
}

data class PSourceRootKotlinOptions(
    val noStdlib: Boolean?,
    val noReflect: Boolean?,
    val moduleName: String?,
    val apiVersion: String?,
    val languageVersion: String?,
    val jvmTarget: String?,
    val addCompilerBuiltIns: Boolean?,
    val loadBuiltInsFromDependencies: Boolean?,
    val extraArguments: List<String>
) {
    fun intersect(other: PSourceRootKotlinOptions) = PSourceRootKotlinOptions(
        if (noStdlib == other.noStdlib) noStdlib else null,
        if (noReflect == other.noReflect) noReflect else null,
        if (moduleName == other.moduleName) moduleName else null,
        if (apiVersion == other.apiVersion) apiVersion else null,
        if (languageVersion == other.languageVersion) languageVersion else null,
        if (jvmTarget == other.jvmTarget) jvmTarget else null,
        if (addCompilerBuiltIns == other.addCompilerBuiltIns) addCompilerBuiltIns else null,
        if (loadBuiltInsFromDependencies == other.loadBuiltInsFromDependencies) loadBuiltInsFromDependencies else null,
        extraArguments.intersect(other.extraArguments).toList()
    )
}

data class POrderRoot(
    val dependency: PDependency,
    val scope: Scope,
    val isExported: Boolean = false,
    val isProductionOnTestDependency: Boolean = false
) {
    enum class Scope { COMPILE, TEST, RUNTIME, PROVIDED }
}

sealed class PDependency {
    data class Module(val name: String) : PDependency()
    data class Library(val name: String) : PDependency()
    data class ModuleLibrary(val library: PLibrary) : PDependency()
}

data class PLibrary(
    val name: String,
    val classes: List<File>,
    val javadoc: List<File> = emptyList(),
    val sources: List<File> = emptyList(),
    val annotations: List<File> = emptyList(),
    val dependencies: List<PLibrary> = emptyList()
) {
    fun attachSource(file: File): PLibrary {
        return this.copy(sources = this.sources + listOf(file))
    }
}

fun parse(project: Project, libraries: List<PLibrary>, context: ParserContext): PProject = with (context) {
    if (project != project.rootProject) {
        error("$project is not a root project")
    }

    fun Project.matchesSelectedVariant(): Boolean {
        val extension = this.extensions.findByType(PillExtension::class.java) ?: return true
        val projectVariant = extension.variant.takeUnless { it == Variant.DEFAULT } ?: Variant.BASE
        return projectVariant in context.variant.includes
    }

    val (includedProjects, excludedProjects) = project.allprojects
        .partition { it.plugins.hasPlugin(JpsCompatiblePlugin::class.java) && it.matchesSelectedVariant() }

    val modules = includedProjects.flatMap { parseModules(it, excludedProjects) }

    return PProject("Kotlin", project.projectDir, modules, libraries)
}

/*
    Ordering here and below is significant.
    Placing 'runtime' configuration dependencies on the top make 'idea' tests to run normally.
    ('idea' module has 'intellij-core' as transitive dependency, and we really need to get rid of it.)
 */
private val CONFIGURATION_MAPPING = mapOf(
    listOf("runtime") to Scope.RUNTIME,
    listOf("compile") to Scope.COMPILE,
    listOf("compileOnly") to Scope.PROVIDED
)

private val TEST_CONFIGURATION_MAPPING = mapOf(
    listOf("runtime", "testRuntime") to Scope.RUNTIME,
    listOf("compile", "testCompile") to Scope.COMPILE,
    listOf("compileOnly", "testCompileOnly") to Scope.PROVIDED,
    listOf("jpsTest") to Scope.TEST
)

private fun ParserContext.parseModules(project: Project, excludedProjects: List<Project>): List<PModule> {
    val (productionContentRoots, testContentRoots) = parseContentRoots(project).partition { !it.forTests }

    val modules = mutableListOf<PModule>()

    fun getJavaExcludedDirs() = project.plugins.findPlugin(IdeaPlugin::class.java)
        ?.model?.module?.excludeDirs?.toList() ?: emptyList()

    fun getPillExcludedDirs() = project.extensions.getByType(PillExtension::class.java).excludedDirs

    val allExcludedDirs = getPillExcludedDirs() + getJavaExcludedDirs() + project.buildDir +
            (if (project == project.rootProject) excludedProjects.map { it.buildDir } else emptyList())

    var productionSourcesModule: PModule? = null

    fun getModuleFile(suffix: String = ""): File {
        val relativePath = File(project.projectDir, project.name + suffix + ".iml")
            .toRelativeString(project.rootProject.projectDir)

        return File(project.rootProject.projectDir, ".idea/modules/$relativePath")
    }

    for ((nameSuffix, roots) in mapOf(".src" to productionContentRoots, ".test" to testContentRoots)) {
        if (roots.isEmpty()) {
            continue
        }

        val mainRoot = roots.first()

        var dependencies = parseDependencies(project, mainRoot.forTests)
        if (productionContentRoots.isNotEmpty() && mainRoot.forTests) {
            val productionModuleDependency = PDependency.Module(project.name + ".src")
            dependencies += POrderRoot(productionModuleDependency, Scope.COMPILE, true)
        }

        val module = PModule(
            project.name + nameSuffix,
            project.name,
            mainRoot.path,
            getModuleFile(nameSuffix),
            roots,
            dependencies,
            productionSourcesModule
        )

        modules += module

        if (!mainRoot.forTests) {
            productionSourcesModule = module
        }
    }

    val mainModuleFileRelativePath = when (project) {
        project.rootProject -> File(project.rootProject.projectDir, project.name + ".iml")
        else -> getModuleFile()
    }

    modules += PModule(
        project.name,
        project.name,
        project.projectDir,
        mainModuleFileRelativePath,
        listOf(PContentRoot(project.projectDir, false, emptyList(), allExcludedDirs)),
        if (modules.isEmpty()) parseDependencies(project, false) else emptyList()
    )

    return modules
}

private fun parseContentRoots(project: Project): List<PContentRoot> {
    val sourceRoots = parseSourceRoots(project).groupBy { it.kind }
    fun getRoots(kind: PSourceRoot.Kind) = sourceRoots[kind] ?: emptyList()

    val productionSourceRoots = getRoots(Kind.PRODUCTION) + getRoots(Kind.RESOURCES)
    val testSourceRoots = getRoots(Kind.TEST) + getRoots(Kind.TEST_RESOURCES)

    fun createContentRoots(sourceRoots: List<PSourceRoot>, forTests: Boolean): List<PContentRoot> {
        return sourceRoots.map { PContentRoot(it.path, forTests, listOf(it), emptyList()) }
    }

    return createContentRoots(productionSourceRoots, forTests = false) +
            createContentRoots(testSourceRoots, forTests = true)
}

private fun parseSourceRoots(project: Project): List<PSourceRoot> {
    if (!project.plugins.hasPlugin(JavaPlugin::class.java)) {
        return emptyList()
    }

    val kotlinTasksBySourceSet = project.tasks
            .filter { it.name.startsWith("compile") && it.name.endsWith("Kotlin") }
            .associateBy { it.invokeInternal("getSourceSetName") }

    val sourceRoots = mutableListOf<PSourceRoot>()

    for (sourceSet in project.sourceSets) {
        val kotlinCompileTask = kotlinTasksBySourceSet[sourceSet.name]
        val kind = if (sourceSet.name == SourceSet.TEST_SOURCE_SET_NAME) Kind.TEST else Kind.PRODUCTION

        fun Any.getKotlin(): SourceDirectorySet {
            val kotlinMethod = javaClass.getMethod("getKotlin")
            val oldIsAccessible = kotlinMethod.isAccessible
            try {
                kotlinMethod.isAccessible = true
                return kotlinMethod(this) as SourceDirectorySet
            } finally {
                kotlinMethod.isAccessible = oldIsAccessible
            }
        }

        val kotlinSourceDirectories = (sourceSet as HasConvention).convention
            .plugins["kotlin"]?.getKotlin()?.srcDirs
                ?: emptySet()

        val directories = (sourceSet.java.sourceDirectories.files + kotlinSourceDirectories).toList()
            .filter { it.exists() }
            .takeIf { it.isNotEmpty() }
                ?: continue

        val kotlinOptions = kotlinCompileTask?.let { getKotlinOptions(it) }

        for (resourceRoot in sourceSet.resources.sourceDirectories.files) {
            if (!resourceRoot.exists() || resourceRoot in directories) {
                continue
            }

            val resourceRootKind = when (kind) {
                Kind.PRODUCTION -> Kind.RESOURCES
                Kind.TEST -> Kind.TEST_RESOURCES
                else -> error("Invalid source root kind $kind")
            }

            sourceRoots += PSourceRoot(resourceRoot, resourceRootKind, kotlinOptions)
        }

        for (directory in directories) {
            sourceRoots += PSourceRoot(directory, kind, kotlinOptions)
        }

        for (root in parseResourceRootsProcessedByProcessResourcesTask(project, sourceSet)) {
            if (sourceRoots.none { it.path == root.path }) {
                sourceRoots += root
            }
        }
    }

    return sourceRoots
}

private fun parseResourceRootsProcessedByProcessResourcesTask(project: Project, sourceSet: SourceSet): List<PSourceRoot> {
    val isMainSourceSet = sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME
    val isTestSourceSet = sourceSet.name == SourceSet.TEST_SOURCE_SET_NAME

    val resourceRootKind = if (isTestSourceSet) PSourceRoot.Kind.TEST_RESOURCES else PSourceRoot.Kind.RESOURCES
    val taskNameBase = "processResources"
    val taskName = if (isMainSourceSet) taskNameBase else sourceSet.name + taskNameBase.capitalize()
    val task = project.tasks.findByName(taskName) as? ProcessResources ?: return emptyList()

    val roots = mutableListOf<File>()
    fun collectRoots(spec: CopySpecInternal) {
        if (spec is SingleParentCopySpec && spec.children.none()) {
            roots += spec.sourcePaths.map { File(project.projectDir, it.toString()) }.filter { it.exists() }
            return
        }

        spec.children.forEach(::collectRoots)
    }
    collectRoots(task.rootSpec)

    return roots.map { PSourceRoot(it, resourceRootKind, null) }
}

private fun getKotlinOptions(kotlinCompileTask: Any): PSourceRootKotlinOptions? {
    val compileArguments = kotlinCompileTask.invokeInternal("getSerializedCompilerArguments") as List<String>
    fun parseBoolean(name: String) = compileArguments.contains("-$name")
    fun parseString(name: String) = compileArguments.dropWhile { it != "-$name" }.drop(1).firstOrNull()

    val addCompilerBuiltins = "Xadd-compiler-builtins"
    val loadBuiltinsFromDependencies = "Xload-builtins-from-dependencies"

    fun isOptionForScriptingCompilerPlugin(option: String)
            = option.startsWith("-Xplugin=") && option.contains("kotlin-scripting-compiler")

    val extraArguments = compileArguments.filter {
        it.startsWith("-X") && !isOptionForScriptingCompilerPlugin(it)
                && it != "-$addCompilerBuiltins" && it != "-$loadBuiltinsFromDependencies"
    }

    return PSourceRootKotlinOptions(
            parseBoolean("no-stdlib"),
            parseBoolean("no-reflect"),
            parseString("module-name"),
            parseString("api-version"),
            parseString("language-version"),
            parseString("jvm-target"),
            parseBoolean(addCompilerBuiltins),
            parseBoolean(loadBuiltinsFromDependencies),
            extraArguments
    )
}

private fun Any.invokeInternal(name: String, instance: Any = this): Any? {
    val method = javaClass.methods.single { it.name.startsWith(name) && it.parameterTypes.isEmpty() }

    val oldIsAccessible = method.isAccessible
    try {
        method.isAccessible = true
        return method.invoke(instance)
    } finally {
        method.isAccessible = oldIsAccessible
    }
}

private fun ParserContext.parseDependencies(project: Project, forTests: Boolean): List<POrderRoot> {
    val configurationMapping = if (forTests) TEST_CONFIGURATION_MAPPING else CONFIGURATION_MAPPING

    with(project.configurations) {
        val mainRoots = mutableListOf<POrderRoot>()
        val deferredRoots = mutableListOf<POrderRoot>()

        fun collectConfigurations(): List<CollectedConfiguration> {
            val configurations = mutableListOf<CollectedConfiguration>()

            for ((configurationNames, scope) in configurationMapping) {
                for (configurationName in configurationNames) {
                    val configuration = findByName(configurationName)?.also { it.resolve() } ?: continue

                    val extraDependencies = resolveExtraDependencies(configuration)
                    configurations += CollectedConfiguration(configuration.resolvedConfiguration, scope, extraDependencies)
                }
            }

            return configurations
        }

        nextDependency@ for (dependencyInfo in collectConfigurations().collectDependencies()) {
            val scope = dependencyInfo.scope

            if (dependencyInfo is DependencyInfo.CustomDependencyInfo) {
                val files = dependencyInfo.files
                val library = PLibrary(files.firstOrNull()?.nameWithoutExtension ?: "unnamed", classes = files)

                mainRoots += POrderRoot(PDependency.ModuleLibrary(library), scope)
                continue
            }

            val dependency = (dependencyInfo as DependencyInfo.ResolvedDependencyInfo).dependency

            for (mapper in dependencyMappers) {
                if (dependency.configuration in mapper.configurations && mapper.predicate(dependency)) {
                    val mappedDependency = mapper.mapping(dependency)

                    if (mappedDependency != null) {
                        val mainDependency = mappedDependency.main
                        if (mainDependency != null) {
                            mainRoots += POrderRoot(mainDependency, scope)
                        }

                        for (deferredDep in mappedDependency.deferred) {
                            deferredRoots += POrderRoot(deferredDep, scope)
                        }
                    }

                    continue@nextDependency
                }
            }

            mainRoots += if (dependency.configuration == "runtimeElements" && scope != Scope.TEST) {
                POrderRoot(PDependency.Module(dependency.moduleName + ".src"), scope)
            } else if (dependency.configuration == "tests-jar" || dependency.configuration == "jpsTest") {
                POrderRoot(
                    PDependency.Module(dependency.moduleName + ".test"),
                    scope,
                    isProductionOnTestDependency = true
                )
            } else {
                val classes = dependency.moduleArtifacts.map { it.file }
                val library = PLibrary(dependency.moduleName, classes)
                POrderRoot(PDependency.ModuleLibrary(library), scope)
            }
        }

        return removeDuplicates(mainRoots + deferredRoots)
    }
}

private fun resolveExtraDependencies(configuration: Configuration): List<File> {
    return configuration.dependencies
        .filterIsInstance<SelfResolvingDependency>()
        .map { it.resolve() }
        .filter { isGradleApiDependency(it) }
        .flatMap { it }
}

private fun isGradleApiDependency(files: Iterable<File>): Boolean {
    return listOf("gradle-api", "groovy-all").all { dep ->
        files.any { it.extension == "jar" && it.name.startsWith("$dep-") }
    }
}

private fun removeDuplicates(roots: List<POrderRoot>): List<POrderRoot> {
    val dependenciesByScope = roots.groupBy { it.scope }.mapValues { it.value.mapTo(mutableSetOf()) { it.dependency } }
    fun dependenciesFor(scope: Scope) = dependenciesByScope[scope] ?: emptySet<PDependency>()

    val result = mutableSetOf<POrderRoot>()
    for (root in roots.distinct()) {
        val scope = root.scope
        val dependency = root.dependency

        if (root in result) {
            continue
        }

        if ((scope == Scope.PROVIDED || scope == Scope.RUNTIME) && dependency in dependenciesFor(Scope.COMPILE)) {
            continue
        }

        if (scope == Scope.PROVIDED && dependency in dependenciesFor(Scope.RUNTIME)) {
            result += POrderRoot(dependency, Scope.COMPILE)
            continue
        }

        if (scope == Scope.RUNTIME && dependency in dependenciesFor(Scope.PROVIDED)) {
            result += POrderRoot(dependency, Scope.COMPILE)
            continue
        }

        result += root
    }

    return result.toList()
}

data class CollectedConfiguration(
    val configuration: ResolvedConfiguration,
    val scope: Scope,
    val extraDependencies: List<File> = emptyList())

sealed class DependencyInfo(val scope: Scope) {
    class ResolvedDependencyInfo(scope: Scope, val dependency: ResolvedDependency) : DependencyInfo(scope)
    class CustomDependencyInfo(scope: Scope, val files: List<File>) : DependencyInfo(scope)
}

fun List<CollectedConfiguration>.collectDependencies(): List<DependencyInfo> {
    val dependencies = mutableListOf<DependencyInfo>()

    val unprocessed = LinkedList<DependencyInfo>()
    val existing = mutableSetOf<Pair<Scope, ResolvedDependency>>()

    for ((configuration, scope, extraDependencies) in this) {
        for (dependency in configuration.firstLevelModuleDependencies) {
            unprocessed += DependencyInfo.ResolvedDependencyInfo(scope, dependency)
        }

        if (!extraDependencies.isEmpty()) {
            unprocessed += DependencyInfo.CustomDependencyInfo(scope, extraDependencies)
        }
    }

    while (unprocessed.isNotEmpty()) {
        val info = unprocessed.removeAt(0)
        dependencies += info

        info as? DependencyInfo.ResolvedDependencyInfo ?: continue

        val data = Pair(info.scope, info.dependency)
        existing += data

        for (child in info.dependency.children) {
            if (Pair(info.scope, child) in existing) {
                continue
            }

            unprocessed += DependencyInfo.ResolvedDependencyInfo(info.scope, child)
        }
    }

    return dependencies
}

private val Project.sourceSets: SourceSetContainer
    get() {
        lateinit var result: SourceSetContainer
        project.configure<JavaPluginConvention> { result = sourceSets }
        return result
    }