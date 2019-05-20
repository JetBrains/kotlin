import org.apache.tools.ant.filters.LineContains
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.extra
import org.w3c.dom.Attr
import org.w3c.dom.Element
import java.io.File
import java.net.URL
import java.util.*
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.reflect.KFunction

// --------------------------------------------------
// Exported items:
// --------------------------------------------------

// TODO: pack this as a Gradle plugin
val cidrPluginTools: Map<String, KFunction<Any>> = listOf<KFunction<Any>>(
        ::ideaPluginJarDep,
        ::addIdeaNativeModuleDeps,

        ::packageCidrPlugin,
        ::zipCidrPlugin,
        ::cidrUpdatePluginsXml,

        ::prepareKotlinPluginXml,
        ::preparePluginXml,

        ::pluginJar,
        ::platformDepsJar,

        ::patchFileTemplates,
        ::patchGradleXml
).map { it.name to it }.toMap()

rootProject.extensions.add("cidrPluginTools", cidrPluginTools)

// --------------------------------------------------
// Shared utils:
// --------------------------------------------------

val excludesListFromIdeaPlugin = listOf(
        "lib/android-*.jar", // no need Android stuff
        "lib/kapt3-*.jar", // no annotation processing
        "lib/jps/**", // JSP plugin
        "kotlinc/**"
)

val platformDepsJarName = "kotlinNative-platformDeps.jar"

val pluginXmlPath = "META-INF/plugin.xml"

val Project.includeKotlinUltimate
    get() = rootProject.findProperty("includeKotlinUltimate")?.toString()?.toBoolean() == true

fun Logger.kotlinInfo(message: () -> String) {
    if (isInfoEnabled) { info("[KOTLIN] ${message()}") }
}

// comment out lines in XML files
fun AbstractCopyTask.commentXmlFiles(fileToMarkers: Map<String, List<String>>) {
    val notDone = mutableSetOf<Pair<String, String>>()
    fileToMarkers.forEach { (path, markers) ->
        for (marker in markers) {
            notDone += path to marker
        }
    }

    eachFile {
        val markers = fileToMarkers[this.sourcePath] ?: return@eachFile
        this.filter {
            var data = it
            for (marker in markers) {
                val newData = data.replace(("^(.*" + Pattern.quote(marker) + ".*)$").toRegex(), "<!-- $1 -->")
                data = newData
                notDone -= path to marker
            }
            data
        }

        logger.kotlinInfo {
            "File \"${this.path}\" in task ${this@commentXmlFiles.path} has been patched to comment lines with the following items: $markers"
        }
    }

    doLast {
        check(notDone.size == 0) {
            "Filtering failed for: " +
                    notDone.joinToString(separator = "\n") { (file, marker) -> "file=$file, marker=`$marker`" }
        }
    }
}

// --------------------------------------------------
// CIDR plugin dependencies:
// --------------------------------------------------

fun ideaPluginJarDep(project: Project): Any = with(project) {
    return if (includeKotlinUltimate) {
        // depend on the artifact to be build
        dependencies.project(":prepare:idea-plugin", configuration = "runtimeJar")
    } else {
        // reuse JAR artifact from downloaded plugin
        val ideaPluginForCidrDir: String by rootProject.extra
        fileTree(ideaPluginForCidrDir) {
            include("lib/kotlin-plugin.jar")
        }
    }
}

fun addIdeaNativeModuleDeps(project: Project) = with(project) {
    dependencies {
        if (includeKotlinUltimate) {
            // Gradle projects with Kotlin/Native-specific logic
            // (automatically brings all the necessary transient dependencies, include deps on IntelliJ platform)
            add("compile", project(":idea:idea-native"))
            add("compile", project(":idea:idea-gradle-native"))

            // Detect IDE name and version
            val ideName = if (rootProject.findProperty("intellijUltimateEnabled")?.toString()?.toBoolean() == true) "ideaIU" else "ideaIC" // TODO: what if AndroidStudio?
            val ideVersion = rootProject.extra["versions.intellijSdk"] as String

            // Java APIs (from Big Kotlin project)
            val javaApis = add("compile", "kotlin.build:$ideName:$ideVersion") as ExternalModuleDependency
            with(javaApis) {
                artifact {
                    name = "java-api"
                    type = "jar"
                    extension = "jar"
                }
                artifact {
                    name = "java-impl"
                    type = "jar"
                    extension = "jar"
                }
                isTransitive = false
            }
        } else {
            // contents of Kotlin plugin
            val ideaPluginForCidrDir: String by rootProject.extra
            val ideaPluginJars = fileTree(ideaPluginForCidrDir) {
                exclude(excludesListFromIdeaPlugin)
            }
            add("compile", ideaPluginJars)

            // IntelliJ platform (out of CIDR IDE distribution)
            val cidrIdeDir: String by rootProject.extra
            val cidrPlatform = fileTree(cidrIdeDir) {
                include("lib/*.jar")
                exclude("lib/kotlin*.jar") // because Kotlin should be taken from Kotlin plugin
                exclude("lib/clion*.jar") // don't take scrambled JARs
                exclude("lib/appcode*.jar")
            }
            add("compile", cidrPlatform)

            // standard CIDR plugins
            val cidrPlugins = fileTree(cidrIdeDir) {
                include("plugins/cidr-*/lib/*.jar")
                include("plugins/gradle/lib/*.jar")
            }
            add("compile", cidrPlugins)

            // Java APIs (private artifact that goes together with CIDR IDEs)
            val cidrPlatformDepsDir: String by rootProject.extra
            val cidrPlatformDeps = fileTree(cidrPlatformDepsDir) { include(platformDepsJarName) }
            add("compile", cidrPlatformDeps)
        }
    }
}

// --------------------------------------------------
// CIDR plugin packaging tasks:
// --------------------------------------------------

fun packageCidrPlugin(
        project: Project,
        predecessorProjectName: String,
        cidrPluginDir: File,
        pluginJarTask: Task,
        platformDepsJarTask: Task,
        platformDepsDir: File
): PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> = with(project) {
    tasks.creating(Copy::class) {
        into(cidrPluginDir)

        into("lib") {
            from(pluginJarTask)
            from(platformDepsJarTask)

            val otherPlatformDepsJars = fileTree(platformDepsDir) {
                include("*.jar")
                exclude(platformDepsJarName)
            }.files
            from(otherPlatformDepsJars)
        }

        includeProjectTemplates(project(predecessorProjectName))

        val ideaPluginDir = if (includeKotlinUltimate) {
            dependsOn(":ideaPlugin")
            // use IDEA plugin dir from Big Kotlin
            val ideaPluginDir: File by rootProject.extra
            ideaPluginDir
        } else {
            // use dir where IDEA plugin has been already downloaded
            val ideaPluginForCidrDir: File by rootProject.extra
            ideaPluginForCidrDir
        }

        from(ideaPluginDir) {
            exclude("lib/kotlin-plugin.jar")
            exclude(excludesListFromIdeaPlugin)
        }
    }
}

fun zipCidrPlugin(project: Project, cidrPluginTask: Task, cidrPluginZipPath: File): PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Zip> = with(project) {
    tasks.creating(Zip::class) {
        destinationDirectory.set(cidrPluginZipPath.parentFile)
        archiveFileName.set(cidrPluginZipPath.name)

        from(cidrPluginTask)
        into("Kotlin")

        doLast {
            logger.lifecycle("Plugin artifacts packed to $cidrPluginZipPath")
        }
    }
}

fun cidrUpdatePluginsXml(
        project: Project,
        pluginXmlTask: Task,
        cidrProductFriendlyVersion: String,
        cidrPluginZipPath: File,
        cidrCustomPluginRepoUrl: URL
): NamedDomainObjectContainerCreatingDelegateProvider<Task> = with(project) {
    tasks.creating {
        dependsOn(pluginXmlTask)

        val updatePluginsXmlFile = cidrPluginZipPath.parentFile.resolve("updatePlugins-$cidrProductFriendlyVersion.xml")
        outputs.file(updatePluginsXmlFile)

        val cidrPluginZipDeploymentUrl = URL(cidrCustomPluginRepoUrl, cidrPluginZipPath.name)
        inputs.property("${project.name}-$name-cidrPluginZipDeploymentUrl", cidrPluginZipDeploymentUrl)

        doLast {
            val extractedData = pluginXmlTask.outputs
                    .files
                    .asFileTree
                    .singleFile
                    .extractXmlElements(setOf("id", "version", "description", "idea-version"))

            val id by extractedData
            val version by extractedData
            val description by extractedData

            val ideaVersion = extractedData.getValue("idea-version").second
            val sinceBuild = ideaVersion.getValue("since-build")
            val untilBuild = ideaVersion.getValue("until-build")

            updatePluginsXmlFile.writeText("""
            <plugins>
                <plugin id="${id.first}" url="$cidrPluginZipDeploymentUrl" version="${version.first}">
                    <idea-version since-build="$sinceBuild" until-build="$untilBuild"/>
                    <description>${description.first}</description>
                </plugin>
            </plugins>
           """.trimIndent()
            )

            logger.lifecycle("Custom plugin repository XML descriptor written to $updatePluginsXmlFile")
        }
    }
}

// extract all the necessary XML elements from the file
// fail if either: one of elements not found or some element found more than once
fun File.extractXmlElements(elementTagNames: Set<String>): Map<String, Pair<String, Map<String, String>>> {
    if (elementTagNames.isEmpty()) return emptyMap()

    val result = mutableMapOf<String, Pair<String, Map<String, String>>>()

    val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
    for (tagName in elementTagNames) {
        val elements = document.getElementsByTagName(tagName)
        check(elements.length == 1) { "$this: ${elements.length} \"$tagName\" elements found, expected amount: 1" }

        val element = elements.item(0) as Element

        val attributes = mutableMapOf<String, String>()
        result[tagName] = element.textContent to attributes

        with(element.attributes) {
            for (i in 0 until length) {
                with(item(i) as Attr) {
                    attributes[name] = value
                }
            }
        }
    }

    return result
}

// --------------------------------------------------
// CIDR plugin templates preparation:
// --------------------------------------------------

// All files that will be patched by templating engine:
val cidrProjectTemplatePatchedFiles = setOf("build.gradle.kts", "settings.gradle.kts", "TemplateInfo.plist")

val clionProjectTemplateInfoFile = "template.info"

fun Copy.includeProjectTemplates(sourceProject: Project) {
    val templatesDir = sourceProject.file("templates")
    inputs.dir(templatesDir)

    val templateParameters = project.getTemplateParameters()
    inputs.property("${project.name}-$name-includeProjectTemplates-templateParameters", templateParameters)

    into("templates") {
        from(templatesDir)
        eachFile {
            if (sourceName in cidrProjectTemplatePatchedFiles) {
                exclude()

                try {
                    val rendered = project.renderTemplate(file, templateParameters)

                    val destinationFile = destinationDir.resolve(path)
                    destinationFile.parentFile.mkdirs()
                    destinationFile.writeText(rendered)
                } catch (e: Exception) {
                    logger.error(
                            """
                            |Error rendering template "$path" with parameters:
                            |${templateParameters.entries.joinToString(separator = "") { (name, value) -> "$name=[$value]\n" }}
                            |Caused by : $e
                            """.trimMargin())

                    throw e
                }
            }
        }
    }

    doLast {
        if (didWork) {
            // make some checks
            val destTemplatesDir = destinationDir.resolve("templates")
            project.fileTree(destTemplatesDir)
                    .filter { it.name == clionProjectTemplateInfoFile }
                    .forEach { templateInfoFile ->
                        // check that the template info file is in the proper location
                        val templateDir = templateInfoFile.parentFile
                        check(with(templateDir.parentFile) { name == "gradle" && parentFile == destTemplatesDir }) {
                            "$clionProjectTemplateInfoFile file is in the wrong location: $templateInfoFile"
                        }

                        // check that the files marked with "openInEditor" are actually exist
                        val properties = Properties().apply { templateInfoFile.inputStream().use { load(it) } }
                        val filesToOpen = properties["openInEditor"]?.toString()?.split(',') ?: return@forEach

                        filesToOpen.forEach {
                            val fileToOpen = templateDir.resolve(it)
                            check(fileToOpen.isFile) { "File should be opened in IDE but such file even does not exist: $fileToOpen" }
                        }
                    }
        }
    }
}

enum class ReleaseType {
    DEV, EAP, RELEASE, SNAPSHOT
}

fun Project.getTemplateParameters(): Map<String, String> {
    /*
     * Possible values:
     * 1.3.40-dev-1654
     * 1.3.30-eap-125
     * 1.3.30-release-170
     * 1.3-SNAPSHOT
     */
    val kotlinBuildNumber = kotlinBuildNumberByIdeaPlugin

    val (numericVersion, releaseType) = kotlinBuildNumber.toLowerCase(Locale.US)
            .split('-', limit = 3)
            .takeIf { it.size in 2..3 }
            ?.let { (numericVersion, releaseTypeName) ->
                val releaseType = ReleaseType.values().firstOrNull {
                    it.name.equals(releaseTypeName, ignoreCase = true)
                } ?: return@let null
                numericVersion to releaseType
            } ?: error("Invalid or unsupported kotlin build number format: $kotlinBuildNumber")

    val gradlePluginVersion = when (releaseType) {
        ReleaseType.RELEASE -> numericVersion // leave only numeric part, ex: "1.3.30"
        else -> kotlinBuildNumber // otherwise use the full build number
    }

    return mapOf(
            "MPP_HOST_PLATFORM" to mppPlatform,
            "MPP_GRADLE_PLUGIN_VERSION" to gradlePluginVersion,
            "MPP_CUSTOM_PLUGIN_REPOS_4S" to customPluginRepos(releaseType, kotlinBuildNumber, 4),
            "MPP_CUSTOM_PLUGIN_REPOS_8S" to customPluginRepos(releaseType, kotlinBuildNumber, 8),
            "MPP_PLUGIN_RESOLUTION_RULES" to pluginResolutionRules(releaseType)
    ).also {
        logger.kotlinInfo {
            """
            |Using template parameters for project ${project.path} (${it.size} items):
            |${it.entries.joinToString(separator = "") { (name, value) -> "$name=[$value]\n" }}
            |""".trimMargin()
        }
    }
}

val Project.kotlinBuildNumberByIdeaPlugin: String
    get() = if (includeKotlinUltimate) {
        // take it from Big Kotlin
        val buildNumber: String by rootProject.extra
        buildNumber
    } else {
        val ideaPluginForCidrBuildNumber: String by rootProject.extra
        ideaPluginForCidrBuildNumber
    }

val hostOsName: String
    get() = System.getProperty("os.name")!!.toLowerCase(Locale.US)

// inspired by com.intellij.openapi.util.SystemInfoRt
val mppPlatform: String
    get() = with(hostOsName) {
        when {
            startsWith("windows") -> "mingwX64"
            startsWith("mac") -> "macosX64"
            else -> "linuxX64"
        }
    }

fun customPluginRepos(releaseType: ReleaseType, kotlinBuildNumber: String, indentSpaces: Int): String {
    val repos = when (releaseType) {
        ReleaseType.RELEASE -> emptyList()
        ReleaseType.EAP -> listOf(
                "https://dl.bintray.com/kotlin/kotlin-eap",
                "https://dl.bintray.com/kotlin/kotlin-dev"
        )
        ReleaseType.DEV -> listOf(
                "https://dl.bintray.com/kotlin/kotlin-dev",
                "https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:Kotlin_dev_Compiler),number:$kotlinBuildNumber,branch:default:any/artifacts/content/maven/"
        )
        ReleaseType.SNAPSHOT -> listOf(
                "https://oss.sonatype.org/content/repositories/snapshots"
        )
    }

    return repos.joinToString(separator = "") { "\n${" ".repeat(indentSpaces)}maven(\"$it\")" }
}

fun pluginResolutionRules(releaseType: ReleaseType): String = when (releaseType) {
    ReleaseType.SNAPSHOT -> """
        |
        |    resolutionStrategy {
        |        eachPlugin {
        |            if (requested.id.name == "multiplatform") {
        |                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${'$'}{requested.version}")
        |            }
        |        }
        |    }
        |
        """.trimMargin()
    else -> ""
}

// We shall not use any external templating engine in buildscripts because it can leak to Gradle build classpath.
// Templating engines provided by Groovy runtime can be used with limitations: If there is a symbol starting
// with '$' that is not one of template parameters, then engine will fail.
// So we have to use simple & reliable self-made templating.
fun Project.renderTemplate(template: File, templateParameters: Map<String, String>): String {
    val templateText = template.readText()
    var result = templateText

    val usedParameters = mutableListOf<String>()
    templateParameters.entries.forEach { (name, value) ->
        val temp = result.replace("@@$name@@", value)
        if (result != temp) usedParameters += name
        result = temp
    }

    if (usedParameters.isEmpty()) {
        logger.kotlinInfo { "Template \"$template\" was not rendered. It does not include any parameters." }
    } else {
        logger.kotlinInfo { "Template \"$template\" was rendered with the following parameters: $usedParameters" }
    }

    if (result.isBlank() && templateText.isNotBlank())
        error("Template rendering resulted is blank string, however template file is not blank: $template")

    return result
}

// --------------------------------------------------
// CIDR plugin XML tasks:
// --------------------------------------------------

// Extract plugin.xml from the original Kotlin plugin, patch this file to exclude non-CIDR stuff and version information,
// and then save under new name KotlinPlugin.xml.
fun prepareKotlinPluginXml(project: Project, originalPluginJar: Configuration): NamedDomainObjectContainerCreatingDelegateProvider<Task> = with(project) {
    tasks.creating {
        val kotlinPluginXmlPath = "META-INF/KotlinPlugin.xml"

        inputs.files(originalPluginJar)
        outputs.dir("$buildDir/$name")

        doFirst {
            val placeholderRegex = Regex(
                    """<!-- CIDR-PLUGIN-PLACEHOLDER-START -->(.*)<!-- CIDR-PLUGIN-PLACEHOLDER-END -->""",
                    RegexOption.DOT_MATCHES_ALL
            )

            val excludeRegex = Regex(
                    """<!-- CIDR-PLUGIN-EXCLUDE-START -->(.*?)<!-- CIDR-PLUGIN-EXCLUDE-END -->""",
                    RegexOption.DOT_MATCHES_ALL
            )

            val ideaVersionRegex = Regex("""<idea-version[^/>]+/>""".trimMargin())

            val versionRegex = Regex("""<version>([^<]+)</version>""")

            zipTree(inputs.files.singleFile)
                    .matching { include(pluginXmlPath) }
                    .singleFile
                    .readText()
                    .replace(placeholderRegex, "<depends>com.intellij.modules.cidr.lang</depends>")
                    .replace(excludeRegex, "")
                    .replace(ideaVersionRegex, "") // IDEA version to be specified in CLion or AppCode plugin.xml file.
                    .replace(versionRegex, "") // Version to be specified in CLion or AppCode plugin.xml file.
                    .also { pluginXmlText ->
                        val pluginXmlFile = File(outputs.files.singleFile, kotlinPluginXmlPath)
                        pluginXmlFile.parentFile.mkdirs()
                        pluginXmlFile.writeText(pluginXmlText)
                    }
        }
    }
}

// Prepare plugin.xml file with the actual version information (kotlin, cidr, plugin).
fun preparePluginXml(
        project: Project,
        predecessorProjectName: String,
        productVersion: String,
        strictProductVersionLimitation: Boolean,
        cidrPluginVersionFull: String
): PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> = with(project) {
    tasks.creating(Copy::class) {
        dependsOn("$predecessorProjectName:assemble")

        inputs.property("${project.name}-$name-strictProductVersionLimitation", strictProductVersionLimitation)
        inputs.property("${project.name}-$name-cidrPluginVersionFull", cidrPluginVersionFull)
        outputs.dir("$buildDir/$name")

        val predecessorProjectResources: File = project(predecessorProjectName)
                .the<JavaPluginConvention>()
                .sourceSets.getByName("main")
                .output
                .resourcesDir as File

        from(predecessorProjectResources, Action { include(pluginXmlPath) })
        into(outputs.files.singleFile)

        applyCidrVersionRestrictions(productVersion, strictProductVersionLimitation, cidrPluginVersionFull)
    }
}

fun Copy.applyCidrVersionRestrictions(
        productVersion: String,
        strictProductVersionLimitation: Boolean,
        cidrPluginVersionFull: String
) {
    val dotsCount = productVersion.count { it == '.' }
    check(dotsCount in 1..2) {
        "Wrong CIDR product version format: $productVersion"
    }

    // private product versions don't have two dots
    val privateProductVersion = dotsCount == 1

    val applyStrictProductVersionLimitation = if (privateProductVersion && strictProductVersionLimitation) {
        // it does not make sense for private versions to apply strict version limitation
        logger.warn("Non-public CIDR product version [$productVersion] has been specified. The corresponding `versions.<product>.strict` property will be ignored.")
        false
    } else strictProductVersionLimitation

    val sinceBuild = if (privateProductVersion)
        productVersion
    else
        productVersion.substringBeforeLast('.')

    val untilBuild = if (applyStrictProductVersionLimitation) {
        // if `strict` then restrict plugin to the same single version of CLion or AppCode
        "$sinceBuild.*"
    } else productVersion.substringBefore('.') + ".*"

    filter {
        it
                .replace("<!--idea_version_placeholder-->",
                        "<idea-version since-build=\"$sinceBuild\" until-build=\"$untilBuild\"/>")
                .replace("<!--version_placeholder-->",
                        "<version>$cidrPluginVersionFull</version>")
    }
}

// --------------------------------------------------
// CIDR plugin JAR tasks:
// --------------------------------------------------

// Prepare Kotlin plugin main JAR file.
fun pluginJar(
        project: Project,
        originalPluginJar: Configuration,
        patchedFilesTasks: List<Task>
): Jar = with(project) {
    val jarTask = tasks.findByName("jar") as Jar? ?: task<Jar>("jar")

    return jarTask.apply {
        // First, include patched files.
        for (t in patchedFilesTasks) {
            dependsOn(t)
            from(t)
        }

        // Only then include contents of original JAR file.
        // Note: If there is a file with the same path inside of JAR file as in the output of one of
        // `patchedFilesTasks`, then the file from JAR will be ignored (due to DuplicatesStrategy.EXCLUDE).
        dependsOn(originalPluginJar)
        from(provider { zipTree(originalPluginJar.singleFile) }) { exclude(pluginXmlPath) }

        configurations.findByName("embedded")?.let { embedded ->
            dependsOn(embedded)
            from(provider { embedded.map(::zipTree) }) { exclude(pluginXmlPath) }
        }

        archiveBaseName.set(project.the<BasePluginConvention>().archivesBaseName)
        archiveFileName.set("kotlin-plugin.jar")
        manifest.attributes.apply {
            put("Implementation-Vendor", "JetBrains")
            put("Implementation-Title", archiveBaseName.get())
        }
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)
    }
}

// Prepare patched "platformDeps" JAR file.
fun platformDepsJar(project: Project, productName: String, platformDepsDir: File): PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Zip> = with(project) {
    tasks.creating(Zip::class) {
        archiveFileName.set("kotlinNative-platformDeps-$productName.jar")
        destinationDirectory.set(file("$buildDir/$name"))

        val platformDepsReplacementsDir = file("resources/platformDeps")
        val platformDepsReplacements = platformDepsReplacementsDir.walkTopDown()
                .filter { it.isFile && it.length() > 0 }
                .map { it.relativeTo(platformDepsReplacementsDir).path }
                .toList()

        inputs.property("${project.name}-$name-platformDepsReplacements-amount", platformDepsReplacements.size)

        platformDepsReplacements.forEach {
            from(platformDepsReplacementsDir) { include(it) }
        }

        from(zipTree(fileTree(platformDepsDir).matching { include(platformDepsJarName) }.singleFile)) {
            exclude(pluginXmlPath)
            platformDepsReplacements.forEach { exclude(it) }
        }

        patchJavaXmls()
    }
}

fun Zip.patchJavaXmls() {
    val javaPsiXmlPath = "META-INF/JavaPsiPlugin.xml"
    val javaPluginXmlPath = "META-INF/JavaPlugin.xml"

    val fileToMarkers = mapOf(
            javaPsiXmlPath to listOf("implementation=\"org.jetbrains.uast.java.JavaUastLanguagePlugin\""),
            javaPluginXmlPath to listOf(
                    "implementation=\"com.intellij.spi.SPIFileTypeFactory\"",
                    "implementationClass=\"com.intellij.lang.java.JavaDocumentationProvider\""
            )
    )

    commentXmlFiles(fileToMarkers)
}


// --------------------------------------------------
// CIDR plugin patches:
// --------------------------------------------------

// See KT-30178
fun patchFileTemplates(project: Project, originalPluginJar: Configuration): PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> = with(project) {
    tasks.creating(Copy::class) {
        val filteredItems = listOf("#parse(\"File Header.java\")")

        from(zipTree(originalPluginJar.singleFile).matching { include("fileTemplates/**/*.ft") })
        destinationDir = file("$buildDir/$name")

        filter(
                mapOf("negate" to true, "contains" to filteredItems),
                LineContains::class.java
        )

        eachFile {
            logger.kotlinInfo {
                "File \"${this.path}\" in task ${this@with.path} has been patched to remove lines with the following items: $filteredItems"
            }
        }
    }
}

// Disable `KotlinMPPGradleProjectTaskRunner` in CIDR plugins
fun patchGradleXml(project: Project, originalPluginJar: Configuration): PolymorphicDomainObjectContainerCreatingDelegateProvider<Task, Copy> = with(project) {
    tasks.creating(Copy::class) {
        val gradleXmlPath = "META-INF/gradle.xml"
        val filteredItems = listOf("implementation=\"org.jetbrains.kotlin.idea.gradle.execution.KotlinMPPGradleProjectTaskRunner\"")

        from(zipTree(originalPluginJar.singleFile).matching { include(gradleXmlPath) })
        destinationDir = file("$buildDir/$name")

        commentXmlFiles(mapOf(gradleXmlPath to filteredItems))
    }
}
