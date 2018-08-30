@file:Suppress("PackageDirectoryMismatch")
package org.jetbrains.kotlin.pill

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePluginConvention
import org.gradle.kotlin.dsl.extra
import shadow.org.jdom2.input.SAXBuilder
import shadow.org.jdom2.*
import shadow.org.jdom2.output.Format
import shadow.org.jdom2.output.XMLOutputter
import java.io.File

class PillConfigurablePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.configurations.create(EmbeddedComponents.CONFIGURATION_NAME)
        project.extensions.create("pill", PillExtension::class.java)
    }
}

class JpsCompatiblePlugin : Plugin<Project> {
    companion object {
        private fun mapper(module: String, vararg configurations: String): DependencyMapper {
            return DependencyMapper("org.jetbrains.kotlin", module, *configurations) { MappedDependency(PDependency.Library(module)) }
        }

        private fun getDependencyMappers(projectLibraries: List<PLibrary>): List<DependencyMapper> {
            val mappersForKotlinLibrariesExeptStdlib = projectLibraries
                .filter { it.name != "kotlin-stdlib" }
                .mapTo(mutableListOf()) { mapper(it.name, "default", "distJar", "runtimeElements") }

            return mappersForKotlinLibrariesExeptStdlib + listOf(
                DependencyMapper("org.jetbrains.kotlin", "kotlin-stdlib", "distJar", "runtimeElements") {
                    MappedDependency(
                        PDependency.Library("kotlin-stdlib"),
                        listOf(PDependency.Library("annotations-13.0"))
                    )
                },
                DependencyMapper("org.jetbrains", "annotations", "default", version = "13.0") {
                    MappedDependency(
                        null,
                        listOf(PDependency.Library("annotations-13.0"))
                    )
                },
                DependencyMapper("org.jetbrains.kotlin", "kotlin-reflect-api", "runtimeElements") {
                    MappedDependency(PDependency.Library("kotlin-reflect"))
                },
                DependencyMapper("org.jetbrains.kotlin", "kotlin-compiler-embeddable", "runtimeJar") { null },
                DependencyMapper("org.jetbrains.kotlin", "kotlin-stdlib-js", "distJar") { null },
                DependencyMapper("org.jetbrains.kotlin", "kotlin-compiler", "runtimeJar") { null },
                DependencyMapper("org.jetbrains.kotlin", "compiler", "runtimeElements") { null },
                DependencyMapper("kotlin.build.custom.deps", "android", "default") { dep ->
                    val (sdkCommon, otherJars) = dep.moduleArtifacts.map { it.file }.partition { it.name == "sdk-common.jar" }
                    val mainLibrary = PDependency.ModuleLibrary(PLibrary(dep.moduleName, otherJars))
                    val deferredLibrary = PDependency.ModuleLibrary(PLibrary(dep.moduleName + "-deferred", sdkCommon))
                    MappedDependency(mainLibrary, listOf(deferredLibrary))
                }
            )
        }

        fun getProjectLibraries(rootProject: Project): List<PLibrary> {
            val distLibDir = File(rootProject.extra["distLibDir"].toString())
            fun distJar(name: String) = File(rootProject.projectDir, "dist/kotlinc/lib/$name.jar")

            val libraries = rootProject.allprojects
                .mapNotNull { library ->
                    val libraryExtension = library.extensions.findByType(PillExtension::class.java)
                            ?.takeIf { it.importAsLibrary }
                            ?: return@mapNotNull null

                    val libraryPath = libraryExtension.libraryPath ?: distLibDir
                    val archivesBaseName = library.convention.findPlugin(BasePluginConvention::class.java)?.archivesBaseName ?: library.name

                    fun List<File>.filterExisting() = filter { it.exists() }

                    PLibrary(
                        library.name,
                        classes = listOf(File(libraryPath, "$archivesBaseName.jar")).filterExisting(),
                        sources = listOf(File(libraryPath, "$archivesBaseName-sources.jar")).filterExisting()
                    )
                }

            return libraries + PLibrary("annotations-13.0", classes = listOf(distJar("annotations-13.0")))
        }
    }

    override fun apply(project: Project) {
        project.plugins.apply(PillConfigurablePlugin::class.java)
        // 'jpsTest' does not require the 'tests-jar' artifact
        project.configurations.create("jpsTest")

        if (project == project.rootProject) {
            project.tasks.create("pill") {
                doLast { pill(project) }

                if (System.getProperty("pill.android.tests", "false") == "true") {
                    TaskUtils.useAndroidSdk(this)
                    TaskUtils.useAndroidJar(this)
                }
            }

            project.tasks.create("unpill") {
                doLast { unpill(project) }
            }
        }
    }

    private lateinit var projectDir: File
    private lateinit var platformVersion: String
    private lateinit var platformBaseNumber: String
    private lateinit var platformDir: File
    private var isAndroidStudioPlatform: Boolean = false

    private fun initEnvironment(project: Project) {
        projectDir = project.projectDir
        platformVersion = project.extensions.extraProperties.get("versions.intellijSdk").toString()
        platformBaseNumber = platformVersion.substringBefore(".", "").takeIf { it.isNotEmpty() }
            ?: platformVersion.substringBefore("-", "").takeIf { it.isNotEmpty() }
            ?: error("Invalid platform version: $platformVersion")
        platformDir = IntellijRootUtils.getIntellijRootDir(project)
        isAndroidStudioPlatform = project.extensions.extraProperties.has("versions.androidStudioRelease")
    }

    private fun pill(rootProject: Project) {
        initEnvironment(rootProject)

        val variantOptionValue = System.getProperty("pill.variant", "base").toUpperCase()
        val variant = PillExtension.Variant.values().firstOrNull { it.name == variantOptionValue }
                ?: run {
                    rootProject.logger.error("Invalid variant name: $variantOptionValue")
                    return
                }

        rootProject.logger.lifecycle("Pill: Setting up project for the '${variant.name.toLowerCase()}' variant...")

        if (variant == PillExtension.Variant.NONE || variant == PillExtension.Variant.DEFAULT) {
            rootProject.logger.error("'none' and 'default' should not be passed as a Pill variant property value")
            return
        }

        val projectLibraries = getProjectLibraries(rootProject)
        val parserContext = ParserContext(getDependencyMappers(projectLibraries), variant)

        val jpsProject = parse(rootProject, projectLibraries, parserContext)
            .mapLibraries(this::attachPlatformSources, this::attachAsmSources)

        generateKotlinPluginArtifactFile(rootProject).write()

        val files = render(jpsProject)

        removeExistingIdeaLibrariesAndModules()
        removeJpsRunConfigurations()

        copyRunConfigurations()
        setOptionsForDefaultJunitRunConfiguration(rootProject)

        files.forEach { it.write() }
    }

    private fun unpill(project: Project) {
        initEnvironment(project)

        removeExistingIdeaLibrariesAndModules()
        removeJpsRunConfigurations()
    }

    private fun removeExistingIdeaLibrariesAndModules() {
        File(projectDir, ".idea/libraries").deleteRecursively()
        File(projectDir, ".idea/modules").deleteRecursively()
    }

    private fun removeJpsRunConfigurations() {
        File(projectDir, ".idea/runConfigurations")
            .walk()
            .filter { it.name.startsWith("JPS_") && it.extension.toLowerCase() == "xml" }
            .forEach { it.delete() }
    }

    private fun copyRunConfigurations() {
        val runConfigurationsDir = File(projectDir, "buildSrc/src/main/resources/runConfigurations")
        val targetDir = File(projectDir, ".idea/runConfigurations")
        val platformDirProjectRelative = "\$PROJECT_DIR\$/" + platformDir.toRelativeString(projectDir)
        val additionalIdeaArgs = if (isAndroidStudioPlatform) "-Didea.platform.prefix=AndroidStudio" else ""

        targetDir.mkdirs()

        fun substitute(text: String): String {
            return text
                .replace("\$IDEA_HOME_PATH\$", platformDirProjectRelative)
                .replace("\$ADDITIONAL_IDEA_ARGS\$", additionalIdeaArgs)
        }

        runConfigurationsDir.listFiles()
            .filter { it.extension == "xml" }
            .map { it.name to substitute(it.readText()) }
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
                var options = vmParams.getAttributeValue("value", "")
                    .split(' ')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                fun addOrReplaceOptionValue(name: String, value: Any?) {
                    val optionsWithoutNewValue = options.filter { !it.startsWith("-D$name=") }
                    options = if (value == null) optionsWithoutNewValue else (optionsWithoutNewValue + listOf("-D$name=$value"))
                }

                val robolectricClasspath = project.rootProject
                    .project(":plugins:android-extensions-compiler")
                    .configurations.getByName("robolectricClasspath")
                    .files.joinToString(File.pathSeparator)

                if (options.none { it == "-ea" }) {
                    options += "-ea"
                }

                addOrReplaceOptionValue("idea.home.path", platformDirProjectRelative)
                addOrReplaceOptionValue("ideaSdk.androidPlugin.path", platformDirProjectRelative + "/plugins/android/lib")
                addOrReplaceOptionValue("robolectric.classpath", robolectricClasspath)
                addOrReplaceOptionValue("use.pill", "true")

                val isAndroidStudioBunch = project.findProperty("versions.androidStudioRelease") != null
                addOrReplaceOptionValue("idea.platform.prefix", if (isAndroidStudioBunch) "AndroidStudio" else null)

                val androidJarPath = project.configurations.findByName("androidJar")?.singleFile
                val androidSdkPath = project.configurations.findByName("androidSdk")?.singleFile

                if (androidJarPath != null && androidSdkPath != null) {
                    addOrReplaceOptionValue("android.sdk", "\$PROJECT_DIR\$/" + androidSdkPath.toRelativeString(projectDir))
                    addOrReplaceOptionValue("android.jar", "\$PROJECT_DIR\$/" + androidJarPath.toRelativeString(projectDir))
                }

                vmParams.setAttribute("value", options.joinToString(" "))
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
        val platformSourcesJar = File(platformDir, "../sources/ideaIC-$platformVersion-sources.jar")

        if (library.classes.any { it.startsWith(platformDir) }) {
            return library.attachSource(platformSourcesJar)
        }

        return library
    }

    private fun attachAsmSources(library: PLibrary): PLibrary {
        val asmSourcesJar = File(platformDir, "../asm-shaded-sources/asm-src-$platformBaseNumber.jar")
        val asmAllJar = File(platformDir, "lib/asm-all.jar")

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