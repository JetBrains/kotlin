/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
    val rootDirectory: File,
    val moduleFile: File,
    val contentRoots: List<PContentRoot>,
    val orderRoots: List<POrderRoot>,
    val moduleForProductionSources: PModule? = null
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
    val extraArguments: List<String>
) {
    fun intersect(other: PSourceRootKotlinOptions) = PSourceRootKotlinOptions(
        if (noStdlib == other.noStdlib) noStdlib else null,
        if (noReflect == other.noReflect) noReflect else null,
        if (moduleName == other.moduleName) moduleName else null,
        if (apiVersion == other.apiVersion) apiVersion else null,
        if (languageVersion == other.languageVersion) languageVersion else null,
        if (jvmTarget == other.jvmTarget) jvmTarget else null,
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
    val dependencies: List<PLibrary> = emptyList(),
    val originalName: String = name
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
    listOf("runtimeClasspath") to Scope.RUNTIME,
    listOf("compileClasspath") to Scope.PROVIDED,
    listOf("embedded") to Scope.COMPILE
)

private val TEST_CONFIGURATION_MAPPING = mapOf(
    listOf("runtimeClasspath", "testRuntimeClasspath") to Scope.RUNTIME,
    listOf("compileClasspath", "testCompileClasspath") to Scope.PROVIDED,
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
        val relativePath = File(project.projectDir, project.pillModuleName + suffix + ".iml")
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
            val productionModuleDependency = PDependency.Module(project.pillModuleName + ".src")
            dependencies += POrderRoot(productionModuleDependency, Scope.COMPILE, true)
        }

        val module = PModule(
            project.pillModuleName + nameSuffix,
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
        project.rootProject -> File(project.rootProject.projectDir, project.rootProject.name + ".iml")
        else -> getModuleFile()
    }

    modules += PModule(
        project.pillModuleName,
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

    val kotlinTasksBySourceSet = project.tasks.names
            .filter { it.startsWith("compile") && it.endsWith("Kotlin") }
            .map { project.tasks.getByName(it) }
            .associateBy { it.invokeInternal("getSourceSetName") }

    val sourceRoots = mutableListOf<PSourceRoot>()

    val sourceSets = project.sourceSets
    if (sourceSets != null) {
        for (sourceSet in sourceSets) {
            val kotlinCompileTask = kotlinTasksBySourceSet[sourceSet.name]
            val kind = if (sourceSet.isTestSourceSet) Kind.TEST else Kind.PRODUCTION

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
    }

    return sourceRoots
}

private fun parseResourceRootsProcessedByProcessResourcesTask(project: Project, sourceSet: SourceSet): List<PSourceRoot> {
    val isMainSourceSet = sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME

    val resourceRootKind = if (sourceSet.isTestSourceSet) PSourceRoot.Kind.TEST_RESOURCES else PSourceRoot.Kind.RESOURCES
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

private val SourceSet.isTestSourceSet: Boolean
    get() = name == SourceSet.TEST_SOURCE_SET_NAME
            || name.endsWith("Test")
            || name.endsWith("Tests")

private fun getKotlinOptions(kotlinCompileTask: Any): PSourceRootKotlinOptions? {
    val compileArguments = run {
        val method = kotlinCompileTask::class.java.getMethod("getSerializedCompilerArguments")
        method.isAccessible = true
        method.invoke(kotlinCompileTask) as List<String>
    }

    fun parseBoolean(name: String) = compileArguments.contains("-$name")
    fun parseString(name: String) = compileArguments.dropWhile { it != "-$name" }.drop(1).firstOrNull()

    fun isOptionForScriptingCompilerPlugin(option: String)
            = option.startsWith("-Xplugin=") && option.contains("kotlin-scripting-compiler")

    val extraArguments = compileArguments.filter {
        it.startsWith("-X") && !isOptionForScriptingCompilerPlugin(it)
    }

    return PSourceRootKotlinOptions(
            parseBoolean("no-stdlib"),
            parseBoolean("no-reflect"),
            parseString("module-name"),
            parseString("api-version"),
            parseString("language-version"),
            parseString("jvm-target"),
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
    val configurations = project.configurations
    val configurationMapping = if (forTests) TEST_CONFIGURATION_MAPPING else CONFIGURATION_MAPPING

    val mainRoots = mutableListOf<POrderRoot>()
    val deferredRoots = mutableListOf<POrderRoot>()

    for ((configurationNames, scope) in configurationMapping) {
        for (configurationName in configurationNames) {
            val configuration = configurations.findByName(configurationName) ?: continue
            val (main, deferred) = project.resolveDependencies(configuration, forTests, dependencyMappers)
            mainRoots += main.map { POrderRoot(it, scope) }
            deferredRoots += deferred.map { POrderRoot(it, scope) }
        }
    }

    return removeDuplicates(mainRoots + deferredRoots)
}

fun removeDuplicates(roots: List<POrderRoot>): List<POrderRoot> {
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

val Project.pillModuleName: String
    get() = path.removePrefix(":").replace(':', '.')

val Project.sourceSets: SourceSetContainer?
    get() {
        val convention = project.convention.findPlugin(JavaPluginConvention::class.java) ?: return null
        return convention.sourceSets
    }
