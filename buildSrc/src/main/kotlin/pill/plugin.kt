package org.jetbrains.kotlin.pill

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class JpsCompatiblePlugin : Plugin<Project> {
    override fun apply(project: Project) {}
}

class JpsCompatibleRootPlugin : Plugin<Project> {
    companion object {
        private fun mapper(module: String, vararg configurations: String): DependencyMapper {
            return DependencyMapper("org.jetbrains.kotlin", module, *configurations) { MappedDependency(PDependency.Library(module)) }
        }

        private val dependencyMappers = listOf(
            mapper("protobuf-relocated", "default"),
            mapper("kotlin-test-junit", "distJar", "runtimeElements"),
            mapper("kotlin-script-runtime", "distJar", "runtimeElements"),
            mapper("kotlin-reflect", "distJar", "runtimeElements"),
            mapper("kotlin-test-jvm", "distJar", "runtimeElements"),
            DependencyMapper("org.jetbrains.kotlin", "kotlin-stdlib", "distJar", "runtimeElements") {
                MappedDependency(
                    PDependency.Library("kotlin-stdlib"),
                    listOf(PDependency.Library("annotations-13.0"))
                )
            },
            DependencyMapper("org.jetbrains.kotlin", "kotlin-reflect-api", "runtimeElements") {
                MappedDependency(PDependency.Library("kotlin-reflect"))
            },
            DependencyMapper("org.jetbrains.kotlin", "kotlin-compiler-embeddable", "runtimeJar") { null },
            DependencyMapper("org.jetbrains.kotlin", "kotlin-stdlib-js", "distJar") { null },
            DependencyMapper("org.jetbrains.kotlin", "kotlin-compiler", "runtimeJar") { null },
            DependencyMapper("org.jetbrains.kotlin", "compiler", "runtimeElements") { null }
        )

        fun getProjectLibraries(project: PProject): List<PLibrary> {
            fun distJar(name: String) = File(project.rootDirectory, "dist/kotlinc/lib/$name.jar")
            fun projectFile(path: String) = File(project.rootDirectory, path)

            return listOf(
                PLibrary(
                    "kotlin-stdlib",
                    classes = listOf(distJar("kotlin-stdlib")),
                    sources = listOf(distJar("kotlin-stdlib-sources"))
                ),
                PLibrary(
                    "kotlin-reflect",
                    classes = listOf(distJar("kotlin-reflect")),
                    sources = listOf(distJar("kotlin-reflect-sources"))
                ),
                PLibrary(
                    "annotations-13.0",
                    classes = listOf(distJar("annotations-13.0"))
                ),
                PLibrary(
                    "kotlin-test-jvm",
                    classes = listOf(distJar("kotlin-test")),
                    sources = listOf(distJar("kotlin-test-sources"))
                ),
                PLibrary(
                    "kotlin-test-junit",
                    classes = listOf(distJar("kotlin-test-junit")),
                    sources = listOf(distJar("kotlin-test-junit-sources"))
                ),
                PLibrary(
                    "kotlin-script-runtime",
                    classes = listOf(distJar("kotlin-script-runtime")),
                    sources = listOf(distJar("kotlin-script-runtime-sources"))
                ),
                PLibrary(
                    "protobuf-relocated",
                    classes = listOf(projectFile("custom-dependencies/protobuf-relocated/build/libs/protobuf-java-relocated-2.6.1.jar"))
                )
            )
        }

    }

    override fun apply(project: Project) {
        if (project != project.rootProject) {
            error("JpsCompatible root plugin can be applied only to the root project")
        }

        project.tasks.create("pill") {
            this.doLast { pill(project) }
        }
    }

    private lateinit var projectDir: File
    private lateinit var platformVersion: String
    private lateinit var platformDir: File

    private fun pill(project: Project) {
        projectDir = project.projectDir
        platformVersion = project.extensions.extraProperties.get("versions.intellijSdk").toString()
        platformDir = File(projectDir, "buildSrc/prepare-deps/intellij-sdk/build/repo/kotlin.build.custom.deps/$platformVersion")

        val jpsProject = parse(project, ParserContext(dependencyMappers))
            .mapLibraries(attachPlatformSources(platformVersion))

        val files = render(jpsProject, getProjectLibraries(jpsProject))

        File(projectDir, ".idea/libraries").deleteRecursively()

        File(projectDir, ".idea/runConfigurations")
            .walk()
            .filter { it.name.startsWith("JPS_") && it.extension.toLowerCase() == "xml" }
            .forEach { it.delete() }

        copyRunConfigurations()

        for (file in files) {
            val stubFile = file.path
            stubFile.parentFile.mkdirs()
            stubFile.writeText(file.text)
        }
    }

    private fun copyRunConfigurations() {
        val runConfigurationsDir = File(projectDir, "buildSrc/src/main/kotlin/pill/runConfigurations")
        val targetDir = File(projectDir, ".idea/runConfigurations")
        val platformDirProjectRelative = "\$PROJECT_DIR\$/" + platformDir.toRelativeString(projectDir)

        targetDir.mkdirs()

        runConfigurationsDir.listFiles()
            .filter { it.extension == "xml" }
            .map { it.name to it.readText().replace("\$IDEA_HOME_PATH\$", platformDirProjectRelative) }
            .forEach { File(targetDir, it.first).writeText(it.second) }
    }

    private fun attachPlatformSources(platformVersion: String) = fun(library: PLibrary): PLibrary {
        val platformSourcesJar = File(platformDir, "sources/ideaIC-$platformVersion-sources.jar")

        if (library.classes.any { it.startsWith(platformDir) }) {
            return library.attachSource(platformSourcesJar)
        }

        return library
    }

    private fun PProject.mapLibraries(vararg mappers: (PLibrary) -> PLibrary): PProject {
        fun mapLibrary(root: POrderRoot): POrderRoot {
            val dependency = root.dependency

            if (dependency is PDependency.ModuleLibrary) {
                val newLibrary = mappers.fold(dependency.library) { lib, mapper -> mapper(lib) }
                return root.copy(dependency = dependency.copy(library = newLibrary))
            }

            return root
        }

        val modules = this.modules.map { it.copy(orderRoots = it.orderRoots.map(::mapLibrary)) }
        return this.copy(modules = modules)
    }
}