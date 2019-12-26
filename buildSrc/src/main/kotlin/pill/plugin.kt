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
        project.configurations.maybeCreate(EmbeddedComponents.CONFIGURATION_NAME)
        project.extensions.create("pill", PillExtension::class.java)
    }
}

class JpsCompatiblePlugin : Plugin<Project> {
    companion object {
        private fun getDependencyMappers(projectLibraries: List<PLibrary>): List<DependencyMapper> {
            val mappersForKotlinLibrariesExeptStdlib = projectLibraries
                .filter { it.name != "kotlin-stdlib" }
                .map { DependencyMapper.forProject(it.originalName) { MappedDependency(PDependency.Library(it.name)) } }

            val disabledModuleMappers = listOf(
                ":kotlin-stdlib-common",
                ":core:builtins",
                ":kotlin-compiler",
                ":kotlin-compiler-embeddable",
                ":kotlin-test:kotlin-test-common",
                ":kotlin-test:kotlin-test-annotations-common"
            ).map { DependencyMapper.forProject(it) { null } }

            return listOf(
                DependencyMapper.forProject(":kotlin-stdlib") {
                    MappedDependency(
                        PDependency.Library("kotlin-stdlib"),
                        listOf(PDependency.Library("annotations-13.0"))
                    )
                },
                DependencyMapper.forProject(":kotlin-test:kotlin-test-jvm") { MappedDependency(PDependency.Library("kotlin-test-jvm")) },
                DependencyMapper.forProject(":kotlin-reflect-api") { MappedDependency(PDependency.Library("kotlin-reflect")) }
            ) + mappersForKotlinLibrariesExeptStdlib + disabledModuleMappers
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
                        sources = listOf(File(libraryPath, "$archivesBaseName-sources.jar")).filterExisting(),
                        originalName = library.path
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
    private lateinit var intellijCoreDir: File
    private var isAndroidStudioPlatform: Boolean = false

    private fun initEnvironment(project: Project) {
        projectDir = project.projectDir
        platformVersion = project.extensions.extraProperties.get("versions.intellijSdk").toString()
        platformBaseNumber = platformVersion.substringBefore(".", "").takeIf { it.isNotEmpty() }
            ?: platformVersion.substringBefore("-", "").takeIf { it.isNotEmpty() }
            ?: error("Invalid platform version: $platformVersion")
        platformDir = IntellijRootUtils.getIntellijRootDir(project)
        intellijCoreDir = File(platformDir.parentFile.parentFile.parentFile, "intellij-core")
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
        val dependencyMappers = getDependencyMappers(projectLibraries)

        val parserContext = ParserContext(dependencyMappers, variant)

        val jpsProject = parse(rootProject, projectLibraries, parserContext)
            .mapLibraries(this::attachPlatformSources, this::attachAsmSources)

        val files = render(jpsProject)

        removeExistingIdeaLibrariesAndModules()
        removeJpsAndPillRunConfigurations()
        removeAllArtifactConfigurations()

        generateKotlinPluginArtifactFile(rootProject, dependencyMappers).write()

        copyRunConfigurations()
        setOptionsForDefaultJunitRunConfiguration(rootProject)

        files.forEach { it.write() }
    }

    private fun unpill(project: Project) {
        initEnvironment(project)

        removeExistingIdeaLibrariesAndModules()
        removeJpsAndPillRunConfigurations()
        removeAllArtifactConfigurations()
    }

    private fun removeExistingIdeaLibrariesAndModules() {
        File(projectDir, ".idea/libraries").deleteRecursively()
        File(projectDir, ".idea/modules").deleteRecursively()
    }

    private fun removeJpsAndPillRunConfigurations() {
        File(projectDir, ".idea/runConfigurations")
            .walk()
            .filter { (it.name.startsWith("JPS_") || it.name.startsWith("Pill_")) && it.extension.toLowerCase() == "xml" }
            .forEach { it.delete() }
    }

    private fun removeAllArtifactConfigurations() {
        File(projectDir, ".idea/artifacts")
            .walk()
            .filter { it.extension.toLowerCase() == "xml" }
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

                if (options.none { it == "-ea" }) {
                    options += "-ea"
                }

                addOrReplaceOptionValue("idea.home.path", platformDirProjectRelative)
                addOrReplaceOptionValue("ideaSdk.androidPlugin.path", platformDirProjectRelative + "/plugins/android/lib")
                addOrReplaceOptionValue("use.jps", "true")
                addOrReplaceOptionValue("kotlinVersion", project.rootProject.extra["kotlinVersion"].toString())

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
        val platformSourcesJar = File(platformDir, "../../../sources/intellij-$platformVersion-sources.jar")

        if (library.classes.any { it.startsWith(platformDir) || it.startsWith(intellijCoreDir) }) {
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