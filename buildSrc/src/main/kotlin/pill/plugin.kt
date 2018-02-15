package org.jetbrains.kotlin.pill

import org.gradle.api.Plugin
import org.gradle.api.Project
import shadow.org.jdom2.input.SAXBuilder
import shadow.org.jdom2.*
import shadow.org.jdom2.output.DOMOutputter
import shadow.org.jdom2.output.Format
import shadow.org.jdom2.output.XMLOutputter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.*
import java.io.File
import java.io.FileOutputStream
import javax.xml.transform.stream.StreamResult

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
    private lateinit var platformBaseNumber: String
    private lateinit var platformDir: File

    private fun pill(project: Project) {
        projectDir = project.projectDir
        platformVersion = project.extensions.extraProperties.get("versions.intellijSdk").toString()
        platformBaseNumber = platformVersion.substringBefore(".", "").takeIf { it.isNotEmpty() }
                ?: error("Invalid platform version: $platformVersion")
        platformDir = File(projectDir, "buildSrc/prepare-deps/intellij-sdk/build/repo/kotlin.build.custom.deps/$platformVersion")

        val jpsProject = parse(project, ParserContext(dependencyMappers))
            .mapLibraries(this::attachPlatformSources, this::attachAsmSources)

        val files = render(jpsProject, getProjectLibraries(jpsProject))

        File(projectDir, ".idea/libraries").deleteRecursively()

        File(projectDir, ".idea/runConfigurations")
            .walk()
            .filter { it.name.startsWith("JPS_") && it.extension.toLowerCase() == "xml" }
            .forEach { it.delete() }

        copyRunConfigurations()
        setOptionsForDefaultJunitRunConfiguration(project)

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

    /*
        This sets a proper (project root) working directory and a "idea.home.path" property to the default JUnit configuration,
        so one does not need to make these changes manually.
     */
    private fun setOptionsForDefaultJunitRunConfiguration(project: Project) {
        val workspaceFile = File(projectDir, ".idea/workspace.xml")
        if (!workspaceFile.exists()) {
            project.logger.warn("${workspaceFile.name} does not exist, JUnit default run configuration was not modified")
            return
        }

        val document = SAXBuilder().build(workspaceFile)
        val rootElement = document.rootElement

        fun Element.getOrCreateChild(name: String, vararg attributes: Pair<String, String>): Element {
            for (child in getChildren(name)) {
                if (attributes.all { (attribute, value) -> child.getAttributeValue(attribute) == value }) {
                    return child
                }
            }

            return Element(name).apply {
                for ((attributeName, value) in attributes) {
                    setAttribute(attributeName, value)
                }

                this@getOrCreateChild.addContent(this@apply)
            }
        }

        val platformDirProjectRelative = "\$PROJECT_DIR\$/" + platformDir.toRelativeString(projectDir)

        val runManagerComponent = rootElement.getOrCreateChild("component", "name" to "RunManager")
        val junitConfiguration = runManagerComponent.getOrCreateChild(
            "configuration",
            "default" to "true",
            "type" to "JUnit",
            "factoryName" to "JUnit"
        )

        junitConfiguration.apply {
            getOrCreateChild("option", "name" to "WORKING_DIRECTORY").setAttribute("value", "file://\$PROJECT_DIR\$")
            getOrCreateChild("option", "name" to "VM_PARAMETERS").also { vmParams ->
                val ideaHomePathOptionKey = "-Didea.home.path="
                val ideaHomePathOption = ideaHomePathOptionKey + platformDirProjectRelative
                val existingOptions = vmParams.getAttributeValue("value", "").split(' ')
                if (existingOptions.none { it.startsWith(ideaHomePathOptionKey) }) {
                    vmParams.setAttribute("value", (vmParams.getAttributeValue("value", "") + " " + ideaHomePathOption).trim())
                }
            }
        }

        val output = XMLOutputter().also {
            it.format = Format.getPrettyFormat().apply {
                setEscapeStrategy { Verifier.isHighSurrogate(it) || it == '"' }
                setIndent("  ")
                setTextMode(Format.TextMode.TRIM)
                setOmitEncoding(false)
                setOmitDeclaration(false)
            }
        }

        val postProcessedXml = output.outputString(document)
            .replace("&#x22;", "&quot;")
            .replace("&#xA;", "&#10;")
            .replace("&#xC;", "&#13;")

        workspaceFile.writeText(postProcessedXml)
    }

    private fun attachPlatformSources(library: PLibrary): PLibrary {
        val platformSourcesJar = File(platformDir, "sources/ideaIC-$platformVersion-sources.jar")

        if (library.classes.any { it.startsWith(platformDir) }) {
            return library.attachSource(platformSourcesJar)
        }

        return library
    }

    private fun attachAsmSources(library: PLibrary): PLibrary {
        val asmSourcesJar = File(platformDir, "asm-shaded-sources/asm-src-$platformBaseNumber.jar")
        val asmAllJar = File(platformDir, "intellij/lib/asm-all.jar")

        if (library.classes.any { it == asmAllJar }) {
            return library.attachSource(asmSourcesJar)
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