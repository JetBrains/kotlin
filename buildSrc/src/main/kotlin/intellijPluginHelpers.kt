@file:Suppress("unused") // usages in build scripts are not tracked properly

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.the
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.dependency.PluginDependency
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository

fun RepositoryHandler.intellijSdkRepo(project: Project): IvyArtifactRepository = ivy {
    val baseDir = File("${project.rootDir.absoluteFile}/buildSrc/prepare-deps/intellij-sdk/build/repo")
    setUrl(baseDir)
    ivyPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module].ivy.xml")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/lib/[artifact](-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/[artifact].jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/[module]/[artifact](-[revision])(-[classifier]).jar")
    artifactPattern("${baseDir.canonicalPath}/[organisation]/[revision]/sources/[artifact]-[revision]-[classifier].[ext]")
}

fun Project.intellijDep(module: String = "intellij") = "kotlin.build.custom.deps:$module:${rootProject.extra["versions.intellijSdk"]}"

fun Project.intellijCoreDep() = intellijDep("intellij-core")

fun ModuleDependency.includeJars(vararg names: String) {
    names.forEach {
        artifact {
            name = it.removeSuffix(".jar")
            extension = "jar"
        }
    }
}

fun ModuleDependency.includeIntellijCoreJarDependencies(project: Project, jarsFilter: (String) -> Boolean = { true }) =
        includeJars(*(project.rootProject.extra["IntellijCoreDependenciesJars"] as List<String>).filter { jarsFilter(it) }.toTypedArray())

fun Project.configureIntellijPlugin(body: (IntelliJPluginExtension.() -> Unit) = {}) {

    plugins.apply("org.jetbrains.intellij")

    configure<IntelliJPluginExtension> {
        version = rootProject.extra["versions.intellij"] as String
        instrumentCode = false
        configureDefaultDependencies = false
        body()
    }

    val intellijTasks = listOf("patchPluginXml", "prepareSandbox", "prepareTestingSandbox",
                               "verifyPlugin", "runIde", "buildPlugin", "publishPlugin")
    intellijTasks.forEach {
        tasks.findByName(it)?.also { it.onlyIf { false }; tasks.remove(it) }
        ?: logger.warn("intellij task $it not found")
    }
    configurations.findByName("archives")?.artifacts?.clear()
}

// reimplementation of helpers from intellij plugin, since it is not convenient to use gradle closures from kotlin build scripts

fun Project.intellij(filter: (PatternFilterable.() -> Unit) = {}): FileTree {
    val jars = the<IntelliJPluginExtension>().ideaDependency?.jarFiles?.takeIf { it.isNotEmpty() }
               ?: throw GradleException("intellij is not (yet) configured. Please note that you should configure intellij dependencies in the afterEvaluate block")
    return files(jars).asFileTree.matching {
        exclude("**/kotlin-*.jar")
    }.matching(filter)
}

fun Project.intellijPlugins(vararg pluginNames: String): ConfigurableFileCollection {
    val selectedPlugins = arrayListOf<PluginDependency>()
    val invalidPlugins = arrayListOf<String>()
    for (pluginName in pluginNames) {
        val plugin = the<IntelliJPluginExtension>().pluginDependencies.find { it.id == pluginName }
        if (plugin == null || plugin.jarFiles.isEmpty()) {
            invalidPlugins.add(pluginName)
        }
        else {
            selectedPlugins.add(plugin)
        }
    }
    if (invalidPlugins.isNotEmpty()) {
        throw GradleException("intellij plugins $invalidPlugins are not (yet) configured or not found. Please note that you should specify plugins in the intellij.plugins property and configure dependencies to them in the afterEvaluate block")
    }
    return project.files(selectedPlugins.map { it.jarFiles })
}

fun Project.intellijPlugin(pluginName: String, filter: (PatternFilterable.() -> Unit) = {}): FileTree {
    val pluginDep = the<IntelliJPluginExtension>().pluginDependencies.find { it.id == pluginName }?.takeIf { it.jarFiles.isNotEmpty() }
                    ?: throw GradleException("intellij plugin '$pluginName' is not (yet) configured or not found. Please note that you should specify plugins in the intellij.plugins property and configure dependencies to them in the afterEvaluate block")
    return project.files(pluginDep.jarFiles).asFileTree.matching(filter)
}

fun Project.intellijExtra(extraName: String, filter: (PatternFilterable.() -> Unit) = {}): FileTree {
    val extraDep = the<IntelliJPluginExtension>().ideaDependency?.extraDependencies?.find { it.name == extraName }?.takeIf { it.jarFiles.isNotEmpty() }
                   ?: throw GradleException("intellij extra artifact '$extraName' is not (yet) configured or not found. Please note that you should specify extra dependencies in the intellij.extraDependencies property and configure dependencies to them in the afterEvaluate block")
    return project.files(extraDep.jarFiles).asFileTree.matching(filter)
}

// frequent combinations

fun Project.intellijCoreJar() = intellijExtra("intellij-core") { include("intellij-core.jar") }
fun Project.intellijCoreJarDependencies(filter: (PatternFilterable.() -> Unit) = {}): FileTree =
        intellij {
            include(rootProject.extra["IntellijCoreDependencies"] as List<String>)
        }
        .matching(filter)
