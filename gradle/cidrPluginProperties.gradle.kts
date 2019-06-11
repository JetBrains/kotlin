@file:Suppress("UNUSED_VARIABLE")

import java.lang.Boolean.parseBoolean
import java.net.URL
import java.util.*

val isStandaloneBuild: Boolean = with(rootProject) {
    findProject(":idea") == null && findProject(":kotlin-ultimate") != null
            || findProject(":prepare-deps:idea-plugin") != null // workaround for buildSrc
}

fun locatePropertiesFile(): File {
    val basePropertiesFilePath = if (isStandaloneBuild) { // in standalone build:
        "versions.properties"
    } else { // in joint build:
        "kotlin-ultimate/versions.properties"
    }

    return rootProject.file(basePropertiesFilePath).takeIf { it.exists() }
            ?: rootProject.file("../$basePropertiesFilePath") // workaround for buildSrc
}

val propertiesFile: File = locatePropertiesFile()

propertiesFile.reader().use {
    val properties = Properties()
    properties.load(it)
    properties.forEach { (k, v) ->
        val key = k.toString()
        val propertyValue = findProperty(key)?.toString()
        rootProject.extra[key] = propertyValue ?: v
    }
}

val prepareDepsPath: File = propertiesFile.parentFile.resolve("buildSrc/prepare-deps")

fun externalDepsDir(depsProjectName: String, suffix: String): File =
        prepareDepsPath.resolve(depsProjectName).resolve("build/external-deps").resolve(suffix)

val clionVersion: String by rootProject.extra(rootProject.extra["versions.clion"] as String)
val clionFriendlyVersion: String by rootProject.extra(cidrProductFriendlyVersion("CLion", clionVersion))
val clionRepo: String by rootProject.extra(rootProject.extra["versions.clion.repo"] as String)
val clionVersionStrict: Boolean by rootProject.extra(rootProject.extra["versions.clion.strict"].toBoolean())
val clionPlatformDepsDir: File by rootProject.extra(externalDepsDir("kotlin-native-platform-deps", "clion-platform-deps-$clionVersion"))
val clionUnscrambledJarDir: File by rootProject.extra(externalDepsDir("kotlin-native-platform-deps", "clion-unscrambled-$clionVersion"))
val clionUseJavaPlugin: Boolean by rootProject.extra(cidrProductBranch(clionVersion) >= 192)

val appcodeVersion: String by rootProject.extra(rootProject.extra["versions.appcode"] as String)
val appcodeFriendlyVersion: String by rootProject.extra(cidrProductFriendlyVersion("AppCode", appcodeVersion))
val appcodeRepo: String by rootProject.extra(rootProject.extra["versions.appcode.repo"] as String)
val appcodeVersionStrict: Boolean by rootProject.extra(rootProject.extra["versions.appcode.strict"].toBoolean())
val appcodePlatformDepsDir: File by rootProject.extra(externalDepsDir("kotlin-native-platform-deps", "appcode-platform-deps-$appcodeVersion"))
val appcodeUnscrambledJarDir: File by rootProject.extra(externalDepsDir("kotlin-native-platform-deps", "appcode-unscrambled-$appcodeVersion"))
val appcodeUseJavaPlugin: Boolean by rootProject.extra(false) // not supported yet

val artifactsForCidrDir: File by rootProject.extra(rootProject.rootDir.resolve("dist/artifacts"))
val clionPluginDir: File by rootProject.extra(artifactsForCidrDir.resolve("clionPlugin/Kotlin"))
val appcodePluginDir: File by rootProject.extra(artifactsForCidrDir.resolve("appcodePlugin/Kotlin"))

if (isStandaloneBuild) { // setup additional properties that are required only when running in standalone mode:
    val useAppCodeForCommon = findProperty("useAppCodeForCommon").toBoolean()
    if (useAppCodeForCommon) {
        val cidrIdeDir: File by rootProject.extra(externalDepsDir("cidr", "appcode-$appcodeVersion"))
        val cidrIdeArtifact: String by rootProject.extra("$appcodeRepo:$appcodeVersion:AppCode-$appcodeVersion.sit")
        val cidrPlatformDepsDir: File by rootProject.extra(appcodePlatformDepsDir)
        val cidrUnscrambledJarDir: File by rootProject.extra(appcodeUnscrambledJarDir)
    }
    else {
        val cidrIdeDir: File by rootProject.extra(externalDepsDir("cidr", "clion-$clionVersion"))
        val cidrIdeArtifact: String by rootProject.extra("$clionRepo:$clionVersion:CLion-$clionVersion.tar.gz")
        val cidrPlatformDepsDir: File by rootProject.extra(clionPlatformDepsDir)
        val cidrUnscrambledJarDir: File by rootProject.extra(clionUnscrambledJarDir)
    }

    val ideaPluginForCidrVersion: String by rootProject.extra(rootProject.extra["versions.ideaPluginForCidr"] as String)
    val ideaPluginForCidrBuildNumber: String by rootProject.extra(
            ideaPluginForCidrVersion.split("-release", limit = 2).takeIf { it.size == 2 }?.let { "${it[0]}-release" }
                    ?: ideaPluginForCidrVersion
    )
    val ideaPluginForCidrIde: String by rootProject.extra(rootProject.extra["versions.ideaPluginForCidr.ide"] as String)
    val ideaPluginForCidrRepo: String by rootProject.extra(rootProject.extra["versions.ideaPluginForCidr.repo"] as String)

    val ideaPluginForCidrDir: File by rootProject.extra(externalDepsDir("idea-plugin", "ideaPlugin-$ideaPluginForCidrBuildNumber-$ideaPluginForCidrIde"))
}

// Note:
// - "appcodePluginNumber" Gradle property can be used to override the default plugin number (SNAPSHOT)
// - "appcodePluginZipPath" Gradle property can be used to override the standard location of packed plugin artifacts
// - "appcodePluginRepoUrl" Gradle property can be used to override the URL of custom plugin repo specified in updatePlugins-*.xml
val appcodePluginNumber: String = findProperty("appcodePluginNumber")?.toString() ?: "SNAPSHOT"
val appcodePluginVersionFull: String by rootProject.extra(cidrPluginVersionFull("AppCode", appcodeVersion, appcodePluginNumber))
val appcodePluginZipPath: File by rootProject.extra(
        propertyAsPath("appcodePluginZipPath")
                ?: defaultCidrPluginZipPath(appcodePluginVersionFull)
)
val appcodeCustomPluginRepoUrl: URL by rootProject.extra(cidrCustomPluginRepoUrl("appcodePluginRepoUrl", appcodePluginZipPath))

// Note:
// - "clionPluginNumber" Gradle property can be used to override the default plugin number (SNAPSHOT)
// - "clionPluginZipPath" Gradle property can be used to override the standard location of packed plugin artifacts
// - "clionPluginRepoUrl" Gradle property can be used to override the URL of custom plugin repo specified in updatePlugins-*.xml
val clionPluginNumber: String = findProperty("clionPluginNumber")?.toString() ?: "SNAPSHOT"
val clionPluginVersionFull: String by rootProject.extra(cidrPluginVersionFull("CLion", clionVersion, clionPluginNumber))
val clionPluginZipPath: File by rootProject.extra(
        propertyAsPath("clionPluginZipPath")
                ?: defaultCidrPluginZipPath(clionPluginVersionFull)
)
val clionCustomPluginRepoUrl: URL by rootProject.extra(cidrCustomPluginRepoUrl("clionPluginRepoUrl", clionPluginZipPath))

fun cidrProductBranch(productVersion: String): Int {
    return productVersion.substringBefore('.').toIntOrNull()
            ?: error("Invalid product version format: $productVersion")
}

fun cidrProductFriendlyVersion(productName: String, productVersion: String): String {
    val productBranch = cidrProductBranch(productVersion)
    val year = 2000 + productBranch / 10
    val majorRelease = productBranch % 10

    return "$productName$year.$majorRelease"
}

fun cidrPluginVersionFull(productName: String, productVersion: String, cidrPluginNumber: String): String {
    val cidrPluginVersion = if (isStandaloneBuild) { // in standalone build:
        val ideaPluginForCidrBuildNumber: String by rootProject.extra
        ideaPluginForCidrBuildNumber
    } else { // in joint build:
        // take it from Big Kotlin
        val buildNumber: String? by rootProject.extra
        buildNumber ?: "unknownBuildNumber"
    }

    return "$cidrPluginVersion-$productName-$productVersion-$cidrPluginNumber"
}

fun propertyAsPath(propertyName: String): File? = findProperty(propertyName)?.let { File(it.toString()).canonicalFile }

fun defaultCidrPluginZipPath(cidrProductVersionFull: String): File =
        artifactsForCidrDir.resolve("kotlin-plugin-$cidrProductVersionFull.zip").canonicalFile

fun cidrCustomPluginRepoUrl(repoUrlPropertyName: String, cidrPluginZipPath: File): URL =
        findProperty(repoUrlPropertyName)?.let {
            with (it.toString()) { URL(if (endsWith('/')) this else "$this/") }
        } ?: cidrPluginZipPath.parentFile.toURI().toURL()

fun Any?.toBoolean(): Boolean = parseBoolean(this?.toString())
