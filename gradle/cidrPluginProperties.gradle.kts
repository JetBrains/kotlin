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

val clionVersion: String = rootProject.extra["versions.clion"] as String
rootProject.extra["clionVersion"] = clionVersion
rootProject.extra["clionFriendlyVersion"] = cidrProductFriendlyVersion("CLion", clionVersion)
rootProject.extra["clionRepo"] = rootProject.extra["versions.clion.repo"] as String
rootProject.extra["clionVersionStrict"] = rootProject.extra["versions.clion.strict"].toBoolean()
rootProject.extra["clionPlatformDepsDir"] = externalDepsDir("kotlin-native-platform-deps", "clion-platform-deps-$clionVersion")
rootProject.extra["clionUnscrambledJarDir"] = externalDepsDir("kotlin-native-platform-deps", "clion-unscrambled-$clionVersion")

val appcodeVersion: String = rootProject.extra["versions.appcode"] as String
rootProject.extra["appcodeVersion"] = appcodeVersion
rootProject.extra["appcodeFriendlyVersion"] = cidrProductFriendlyVersion("AppCode", appcodeVersion)
rootProject.extra["appcodeRepo"] = rootProject.extra["versions.appcode.repo"] as String
rootProject.extra["appcodeVersionStrict"] = rootProject.extra["versions.appcode.strict"].toBoolean()
rootProject.extra["appcodePlatformDepsDir"] = externalDepsDir("kotlin-native-platform-deps", "appcode-platform-deps-$appcodeVersion")
rootProject.extra["appcodeUnscrambledJarDir"] = externalDepsDir("kotlin-native-platform-deps", "appcode-unscrambled-$appcodeVersion")

val artifactsForCidrDir: File = rootProject.rootDir.resolve("dist/artifacts")
rootProject.extra["artifactsForCidrDir"] = artifactsForCidrDir
rootProject.extra["clionPluginDir"] = artifactsForCidrDir.resolve("clionPlugin/Kotlin")
rootProject.extra["appcodePluginDir"] = artifactsForCidrDir.resolve("appcodePlugin/Kotlin")

rootProject.extra["cidrUnscrambledJarDir"] = rootProject.extra["clionUnscrambledJarDir"]

if (isStandaloneBuild) { // setup additional properties that are required only when running in standalone mode:
    val useAppCodeForCommon = findProperty("useAppCodeForCommon").toBoolean()
    if (useAppCodeForCommon) {
        rootProject.extra["cidrIdeDir"] = externalDepsDir("cidr", "appcode-$appcodeVersion")
        rootProject.extra["cidrIdeArtifact"] = "${rootProject.extra["appcodeRepo"]}:$appcodeVersion:AppCode-$appcodeVersion.sit"
        rootProject.extra["cidrPlatformDepsDir"] = rootProject.extra["appcodePlatformDepsDir"]
        rootProject.extra["cidrUnscrambledJarDir"] = rootProject.extra["appcodeUnscrambledJarDir"]
    } 
    else {
        rootProject.extra["cidrIdeDir"] = externalDepsDir("cidr", "clion-$clionVersion")
        rootProject.extra["cidrIdeArtifact"] = "${rootProject.extra["clionRepo"]}:$clionVersion:CLion-$clionVersion.tar.gz"
        rootProject.extra["cidrPlatformDepsDir"] = rootProject.extra["clionPlatformDepsDir"]
    }

    val ideaPluginForCidrVersion: String = rootProject.extra["versions.ideaPluginForCidr"] as String
    val ideaPluginForCidrBuildNumber: String = ideaPluginForCidrVersion
            .split("-release", limit = 2)
            .takeIf { it.size == 2 }
            ?.let { "${it[0]}-release" } ?: ideaPluginForCidrVersion
    val ideaPluginForCidrIde: String = rootProject.extra["versions.ideaPluginForCidr.ide"] as String

    rootProject.extra["ideaPluginForCidrVersion"] = ideaPluginForCidrVersion
    rootProject.extra["ideaPluginForCidrBuildNumber"] = ideaPluginForCidrBuildNumber
    rootProject.extra["ideaPluginForCidrIde"] = ideaPluginForCidrIde
    rootProject.extra["ideaPluginForCidrRepo"] = rootProject.extra["versions.ideaPluginForCidr.repo"]

    rootProject.extra["ideaPluginForCidrDir"] = externalDepsDir("idea-plugin", "ideaPlugin-$ideaPluginForCidrBuildNumber-$ideaPluginForCidrIde")
}

// Note: "appcodePluginNumber" Gradle property can be used to override the default plugin number (SNAPSHOT)
val appcodePluginNumber: String = findProperty("appcodePluginNumber")?.toString() ?: "SNAPSHOT"
val appcodePluginVersionFull: String = cidrPluginVersionFull("AppCode", appcodeVersion, appcodePluginNumber)
rootProject.extra["appcodePluginVersionFull"] = appcodePluginVersionFull
// Note: "appcodePluginZipPath" Gradle property can be used to override the standard location of packed plugin artifacts
val appcodePluginZipPath: File = propertyAsPath("appcodePluginZipPath") ?: defaultCidrPluginZipPath(appcodePluginVersionFull)
rootProject.extra["appcodePluginZipPath"] = appcodePluginZipPath
// Note: "appcodePluginRepoUrl" Gradle property can be used to override the URL of custom plugin repo specified in updatePlugins-*.xml
rootProject.extra["appcodeCustomPluginRepoUrl"] = cidrCustomPluginRepoUrl("appcodePluginRepoUrl", appcodePluginZipPath)

// Note: "clionPluginNumber" Gradle property can be used to override the default plugin number (SNAPSHOT)
val clionPluginNumber: String = findProperty("clionPluginNumber")?.toString() ?: "SNAPSHOT"
val clionPluginVersionFull: String = cidrPluginVersionFull("CLion", clionVersion, clionPluginNumber)
rootProject.extra["clionPluginVersionFull"] = clionPluginVersionFull
// Note: "clionPluginZipPath" Gradle property can be used to override the standard location of packed plugin artifacts
val clionPluginZipPath: File = propertyAsPath("clionPluginZipPath") ?: defaultCidrPluginZipPath(clionPluginVersionFull)
rootProject.extra["clionPluginZipPath"] = clionPluginZipPath
// Note: "clionPluginRepoUrl" Gradle property can be used to override the URL of custom plugin repo specified in updatePlugins-*.xml
rootProject.extra["clionCustomPluginRepoUrl"] = cidrCustomPluginRepoUrl("clionPluginRepoUrl", clionPluginZipPath)

fun cidrProductFriendlyVersion(productName: String, productVersion: String): String {
    val productBranch = productVersion.substringBefore('.').toIntOrNull()
            ?: error("Invalid product version format: $productVersion")
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
