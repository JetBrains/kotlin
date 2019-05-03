@file:Suppress("PackageDirectoryMismatch")

package org.jetbrains.kotlin.pill

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
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

    fun add(children: List<ArtifactElement>) {
        myChildren += children
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

fun generateKotlinPluginArtifactFile(rootProject: Project, dependencyMappers: List<DependencyMapper>): PFile {
    val root = Root()

    fun Project.getProject(name: String) = findProject(name) ?: error("Cannot find project $name")

    val prepareIdeaPluginProject = rootProject.getProject(":prepare:idea-plugin")

    root.add(Directory("kotlinc").apply {
        val kotlincDirectory = rootProject.extra["distKotlinHomeDir"].toString()
        add(DirectoryCopy(File(kotlincDirectory)))
    })

    root.add(Directory("lib").apply {
        val librariesConfiguration = prepareIdeaPluginProject.configurations.getByName("libraries")
        add(getArtifactElements(rootProject, librariesConfiguration, dependencyMappers, false))

        add(Directory("jps").apply {
            val prepareJpsPluginProject = rootProject.getProject(":kotlin-jps-plugin")
            add(Archive(prepareJpsPluginProject.name + ".jar").apply {
                val jpsPluginConfiguration = prepareIdeaPluginProject.configurations.getByName("jpsPlugin")
                add(getArtifactElements(rootProject, jpsPluginConfiguration, dependencyMappers, true))
            })
        })

        add(Archive("kotlin-plugin.jar").apply {
            add(FileCopy(File(rootProject.projectDir, "resources/kotlinManifest.properties")))

            val embeddedConfiguration = prepareIdeaPluginProject.configurations.getByName(EmbeddedComponents.CONFIGURATION_NAME)
            add(getArtifactElements(rootProject, embeddedConfiguration, dependencyMappers, true))
        })
    })

    val artifact = PArtifact("KotlinPlugin", File(rootProject.projectDir, "out/artifacts/Kotlin"), root)
    return PFile(
        File(rootProject.projectDir, ".idea/artifacts/${artifact.artifactName}.xml"),
        artifact.render(ProjectContext(rootProject))
    )
}

private fun getArtifactElements(
    rootProject: Project,
    configuration: Configuration,
    dependencyMappers: List<DependencyMapper>,
    extractDependencies: Boolean
): List<ArtifactElement> {
    val dependencies = rootProject.resolveDependencies(configuration, false, dependencyMappers, withEmbedded = true).join()

    val artifacts = mutableListOf<ArtifactElement>()

    for (dependency in dependencies) {
        when (dependency) {
            is PDependency.Module -> {
                val moduleOutput = ModuleOutput(dependency.name)

                if (extractDependencies) {
                    artifacts += moduleOutput
                } else {
                    artifacts += Archive(dependency.name + ".jar").apply {
                        add(moduleOutput)
                    }
                }
            }
            is PDependency.Library -> artifacts += ProjectLibrary(dependency.name)
            is PDependency.ModuleLibrary -> {
                val files = dependency.library.classes
                artifacts += files.map(if (extractDependencies) ::ExtractedDirectory else ::FileCopy)
            }
        }
    }

    return artifacts
}