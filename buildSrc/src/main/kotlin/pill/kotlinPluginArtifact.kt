@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.pill

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.file.copy.SingleParentCopySpec
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.pill.ArtifactElement.*
import org.jetbrains.kotlin.pill.POrderRoot.*
import java.io.File

class PArtifact(val artifactName: String, val outputDir: File, private val contents: ArtifactElement.Root) {
    fun render(context: PathContext) = xml("component", "name" to "ArtifactManager") {
        xml("artifact", "name" to artifactName) {
            xml("output-path") {
                raw(context(outputDir))
            }

            add(contents.renderRecursively(context))
        }
    }
}

sealed class ArtifactElement {
    private val myChildren = mutableListOf<ArtifactElement>()
    val children get() = myChildren

    fun add(child: ArtifactElement) {
        myChildren += child
    }

    abstract fun render(context: PathContext): xml

    fun renderRecursively(context: PathContext): xml {
        return render(context).apply {
            children.forEach { add(it.renderRecursively(context)) }
        }
    }

    fun getDirectory(path: String): ArtifactElement {
        if (path.isEmpty()) {
            return this
        }

        var current: ArtifactElement = this
        for (segment in path.split("/")) {
            val existing = current.children.firstOrNull { it is Directory && it.name == segment }
            if (existing != null) {
                current = existing
                continue
            }

            current = Directory(segment).also { current.add(it) }
        }

        return current
    }

    class Root : ArtifactElement() {
        override fun render(context: PathContext) = xml("root", "id" to "root")
    }

    data class Directory(val name: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "directory", "name" to name)
    }

    data class Archive(val name: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "archive", "name" to name)
    }

    data class ModuleOutput(val moduleName: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "module-output", "name" to moduleName)
    }

    data class FileCopy(val source: File, val outputFileName: String? = null) : ArtifactElement() {
        override fun render(context: PathContext): xml {
            val args = mutableListOf("id" to "file-copy", "path" to context(source))
            if (outputFileName != null) {
                args += "output-file-name" to outputFileName
            }

            return xml("element", *args.toTypedArray())
        }
    }

    data class DirectoryCopy(val source: File) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "dir-copy", "path" to context(source))
    }

    data class ProjectLibrary(val name: String) : ArtifactElement() {
        override fun render(context: PathContext) = xml("element", "id" to "library", "level" to "project", "name" to name)
    }

    data class ExtractedDirectory(val archive: File, val pathInJar: String = "/") : ArtifactElement() {
        override fun render(context: PathContext) =
            xml("element", "id" to "extracted-dir", "path" to context(archive), "path-in-jar" to pathInJar)
    }
}

fun generateKotlinPluginArtifactFile(rootProject: Project): PFile {
    val mainIdeaPluginTask = rootProject.tasks.getByName("ideaPlugin")
    val gradleArtifactDir = File(rootProject.extra["ideaPluginDir"] as File, "lib")

    val ideaPluginTasks = mainIdeaPluginTask.taskDependencies
        .getDependencies(mainIdeaPluginTask)
        .filter { it.name == "ideaPlugin" }
        .filterIsInstance<Copy>()

    val root = Root()

    // Copy kotlinc directory
    root.add(Directory("kotlinc").apply {
        val kotlincDirectory = rootProject.extra["distKotlinHomeDir"].toString()
        add(DirectoryCopy(File(kotlincDirectory)))
    })

    for (task in ideaPluginTasks) {
        val spec = task.rootSpec.children.filterIsInstance<SingleParentCopySpec>().singleOrNull()
                ?: error("Copy spec is not unique in ${rootProject.name}. Available specs: ${task.rootSpec.children}")

        val sourcePaths = spec.sourcePaths
        for (sourcePath in sourcePaths) {
            if (sourcePath is ShadowJar) {
                if (sourcePath.project.path == ":prepare:idea-plugin") {
                    val kotlinPluginJar = Archive(sourcePath.archiveName).also { root.getDirectory("lib").add(it) }

                    kotlinPluginJar.add(FileCopy(File(rootProject.projectDir, "resources/kotlinManifest.properties")))

                    for (jarFile in sourcePath.project.configurations.getByName("embedded").resolve()) {
                        kotlinPluginJar.add(ExtractedDirectory(jarFile))
                    }

                    @Suppress("UNCHECKED_CAST")
                    for (projectPath in sourcePath.project.extra["projectsToShadow"] as List<String>) {
                        val jpsModuleName = rootProject.findProject(projectPath)!!.name + ".src"
                        kotlinPluginJar.add(ModuleOutput(jpsModuleName))
                    }

                    continue
                }
            }

            fun fileCopySnapshotAware(file: File): FileCopy {
                val SHAPSHOT_JAR_SUFFIX = "-SNAPSHOT.jar"
                if (file.name.endsWith(SHAPSHOT_JAR_SUFFIX)) {
                    return FileCopy(file, file.name.dropLast(SHAPSHOT_JAR_SUFFIX.length).substringBeforeLast("-") + ".jar")
                }

                return FileCopy(file)
            }

            when (sourcePath) {
                is Jar -> {
                    val targetDir = ("lib/" + task.destinationDir.toRelativeString(gradleArtifactDir)).withoutSlash()

                    val archiveForJar = Archive(sourcePath.project.name + ".jar").apply {
                        if (task.project.plugins.hasPlugin(JavaPlugin::class.java)) {
                            add(ModuleOutput(sourcePath.project.name + ".src"))
                        }
                        root.getDirectory(targetDir).add(this)
                    }

                    val embeddedComponents = sourcePath.project.configurations
                        .findByName(EmbeddedComponents.CONFIGURATION_NAME)?.resolvedConfiguration

                    if (embeddedComponents != null) {
                        val configuration = CollectedConfiguration(embeddedComponents, Scope.COMPILE)
                        for (dependencyInfo in listOf(configuration).collectDependencies()) {
                            val dependency = (dependencyInfo as? DependencyInfo.ResolvedDependencyInfo)?.dependency ?: continue

                            if (dependency.isModuleDependency) {
                                archiveForJar.add(ModuleOutput(dependency.moduleName + ".src"))
                            } else if (dependency.configuration == "tests-jar" || dependency.configuration == "jpsTest") {
                                error("Test configurations are not allowed here")
                            } else {
                                for (file in dependency.moduleArtifacts.map { it.file }) {
                                    archiveForJar.add(ExtractedDirectory(file))
                                }
                            }
                        }
                    }
                }
                is Configuration -> {
                    require(sourcePath.name == "sideJars") { "Configurations other than 'sideJars' are not supported" }
                    for (file in sourcePath.resolve()) {
                        root.getDirectory("lib").add(fileCopySnapshotAware(file))
                    }
                }
                else -> error("${task.name} Unexpected task type ${task.javaClass.name}")
            }
        }
    }

    val artifact = PArtifact("KotlinPlugin", File(rootProject.projectDir, "out/artifacts/Kotlin"), root)
    return PFile(
        File(rootProject.projectDir, ".idea/artifacts/${artifact.artifactName}.xml"),
        artifact.render(ProjectContext(rootProject))
    )
}

