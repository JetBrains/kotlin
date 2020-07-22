@file:Suppress("UNUSED_VARIABLE")

import java.lang.Boolean.parseBoolean
import java.net.URL
import java.util.*

val isStandaloneBuild: Boolean by rootProject.extra(with(rootProject) {
    findProject(":idea") == null && findProject(":kotlin-ultimate") != null
            || findProject(":prepare-deps:idea-plugin") != null // workaround for buildSrc
})

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
val clionVersionStrict: Boolean by rootProject.extra(rootProject.extra["versions.clion.strict"].toBoolean())
val clionFriendlyVersion: String by rootProject.extra(cidrProductFriendlyVersion("CLion", clionVersion))
val clionRepo: String = rootProject.extra["versions.clion.repo"] as String
val clionUnscrambledJarArtifact: String by rootProject.extra("org:$clionRepo:$clionVersion:unscrambled/clion.jar")
val clionUnscrambledJarDir: File by rootProject.extra(externalDepsDir("kotlin-native-platform-deps", "clion-unscrambled-$clionVersion"))
val clionJavaPluginDownloadUrl: URL by rootProject.extra(
            URL("https://buildserver.labs.intellij.net/guestAuth/repository/download/$clionRepo/$clionVersion/CL-plugins/java${if (clionVersion.substringBefore('.').toInt() >= 202) "-$clionVersion" else ""}.zip")
)

val kotlinNativeBackendVersion: String by rootProject.extra(rootProject.extra["versions.kotlinNativeBackend"] as String)
val kotlinNativeBackendRepo: String by rootProject.extra(rootProject.extra["versions.kotlinNativeBackend.repo"] as String)
val appcodeVersion: String by rootProject.extra(rootProject.extra["versions.appcode"] as String)
val appcodeVersionStrict: Boolean by rootProject.extra(rootProject.extra["versions.appcode.strict"].toBoolean())
val appcodeFriendlyVersion: String by rootProject.extra(cidrProductFriendlyVersion("AppCode", appcodeVersion))
val appcodeRepo: String = rootProject.extra["versions.appcode.repo"] as String
val appcodeUnscrambledJarArtifact: String by rootProject.extra("org:$appcodeRepo:$appcodeVersion:unscrambled/appcode.jar")
val appcodeUnscrambledJarDir: File by rootProject.extra(externalDepsDir("kotlin-native-platform-deps", "appcode-unscrambled-$appcodeVersion"))
val appcodeJavaPluginDownloadUrl: URL by rootProject.extra(
            URL("https://buildserver.labs.intellij.net/guestAuth/repository/download/$appcodeRepo/$appcodeVersion/OC-plugins/java${if (appcodeVersion.substringBefore('.').toInt() >= 202) "-$appcodeVersion" else ""}.zip")
)
val xCodeCompatPluginVersion by rootProject.extra(rootProject.extra["versions.xcode-compat"] as String)

val cidrVersion: String by rootProject.extra(detectCidrPlatformVersion())


val clionCocoaCommonModule by rootProject.extra("org:$clionRepo:$clionVersion")
val clionCocoaCommonArtifacts: List<String> by rootProject.extra(
    listOf(
        "cocoa-common-binaries/Bridge.framework.tar",
        "cocoa-common-binaries/JBDevice.framework.tar",
        "cocoa-common-binaries/libObjCHelper.dylib"
    )
)
val clionCocoaCommonBinariesDir: File by rootProject.extra(
    externalDepsDir(
        "cocoa-common-binaries",
        "cidr-cocoaCommon-binaries-$clionVersion"
    )
)

if (rootProject.extra.has("versions.nativeDebug")) {
    val nativeDebugVersion: String = rootProject.extra["versions.nativeDebug"] as String
    rootProject.extra["nativeDebugVersion"] = nativeDebugVersion
    rootProject.extra["nativeDebugRepo"] = rootProject.extra["versions.nativeDebug.repo"] as String
    rootProject.extra["nativeDebugPluginDir"] = externalDepsDir("native-debug-plugin", "native-debug-$nativeDebugVersion")
}

if (rootProject.extra.has("versions.lldbFrontend.hash")) {
    val lldbFrontendHash = rootProject.extra["versions.lldbFrontend.hash"] as String
    rootProject.extra["lldbFrontendHash"] = lldbFrontendHash

    rootProject.extra["lldbFrontendLinuxRepo"] = rootProject.extra["versions.lldbFrontend.linux.repo"] as String
    rootProject.extra["lldbFrontendLinuxArtifact"] = rootProject.extra["versions.lldbFrontend.linux.artifact"] as String
    rootProject.extra["lldbFrontendLinuxDir"] = externalDepsDir("lldb-frontend", "linux-$lldbFrontendHash")

    rootProject.extra["lldbFrontendMacosRepo"] = rootProject.extra["versions.lldbFrontend.macos.repo"] as String
    rootProject.extra["lldbFrontendMacosArtifact"] = rootProject.extra["versions.lldbFrontend.macos.artifact"] as String
    rootProject.extra["lldbFrontendMacosDir"] = externalDepsDir("lldb-frontend", "macos-$lldbFrontendHash")

    rootProject.extra["lldbFrontendWindowsRepo"] = rootProject.extra["versions.lldbFrontend.windows.repo"] as String
    rootProject.extra["lldbFrontendWindowsArtifact"] = rootProject.extra["versions.lldbFrontend.windows.artifact"] as String
    rootProject.extra["lldbFrontendWindowsDir"] = externalDepsDir("lldb-frontend", "windows-$lldbFrontendHash")
}

if (rootProject.extra.has("versions.LLDB.framework")) {
    val lldbFrameworkVersion: String = rootProject.extra["versions.LLDB.framework"] as String
    rootProject.extra["lldbFrameworkVersion"] = rootProject.extra["versions.LLDB.framework"] as String
    rootProject.extra["lldbFrameworkRepo"] = rootProject.extra["versions.LLDB.framework.repo"] as String
    rootProject.extra["lldbFrameworkDir"] = externalDepsDir("lldb-framework", "lldb-framework-$lldbFrameworkVersion")
}

val artifactsForCidrDir: File by rootProject.extra(rootProject.rootDir.resolve("dist/artifacts"))
val clionPluginDir: File by rootProject.extra(artifactsForCidrDir.resolve("clionPlugin/Kotlin"))
val appcodePluginDir: File by rootProject.extra(artifactsForCidrDir.resolve("appcodePlugin/Kotlin"))
val kmmPluginDir: File by rootProject.extra(artifactsForCidrDir.resolve("androidStudioPlugin/kmm"))
val mobilePluginDir: File by rootProject.extra(artifactsForCidrDir.resolve("mobilePlugin/Mobile"))

if (isStandaloneBuild) { // setup additional properties that are required only when running in standalone mode:
    if (!rootProject.gradle.startParameter.taskNames.any { it.contains("clion", true) }) {
        val cidrUnscrambledJarDir: File by rootProject.extra(appcodeUnscrambledJarDir)
    }
    else {
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
} else {
    val cidrUnscrambledJarDir: File by rootProject.extra(clionUnscrambledJarDir)
}

val intellijBranch: Int by rootProject.extra(
        when {
            rootProject.extra.has("versions.intellijSdk") -> ijProductBranch(rootProject.extra["versions.intellijSdk"] as String)
            isStandaloneBuild -> ijProductBranch(cidrVersion)
            else -> error("Can't determine effective IntelliJ platform branch")
        }
)

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

// Note:
// - "mobilePluginNumber" Gradle property can be used to override the default plugin number (SNAPSHOT)
// - "mobilePluginZipPath" Gradle property can be used to override the standard location of packed plugin artifacts
// - "mobilePluginRepoUrl" Gradle property can be used to override the URL of custom plugin repo specified in updatePlugins-*.xml
val mobilePluginNumber: String = findProperty("mobilePluginNumber")?.toString() ?: "SNAPSHOT"
val mobilePluginVersionFull: String by rootProject.extra(cidrPluginVersionFull("Mobile", clionVersion, mobilePluginNumber))
val mobilePluginZipPath: File by rootProject.extra(
        propertyAsPath("mobilePluginZipPath")
                ?: defaultCidrPluginZipPath(mobilePluginVersionFull, "mobile")
)
val mobileCustomPluginRepoUrl: URL by rootProject.extra(cidrCustomPluginRepoUrl("clionPluginRepoUrl", clionPluginZipPath))

val excludesListFromIdeaPlugin: List<String> by rootProject.extra(listOf(
        "lib/android-*.jar", // no need Android stuff
        "lib/kapt3-*.jar", // no annotation processing
        "lib/jps/**", // JPS plugin
        "kotlinc/**"
))

fun ijProductBranch(productVersion: String): Int {
    return productVersion.substringBefore(".", productVersion.substringBefore("-"))
        .toIntOrNull() ?: error("Invalid product version format: $productVersion")
}

fun detectCidrPlatformVersion(): String {
    val taskNames = rootProject.gradle.startParameter.taskNames

    var count = 0
    val containsAppCode = taskNames.any { it.contains("appcode", true) }.also { if (it) count += 1 }
    val containsCLion = taskNames.any { it.contains("clion", true) }.also { if (it) count += 1 }
    val containsMobile = taskNames.any { it.contains("mobile", true) }.also { if (it) count += 1 }
    if (count > 1) {
        throw InvalidUserDataException("Only one CIDR-dependent artifact can be built in a single run to avoid ambiguity of dependencies")
    }
    return when {
        containsAppCode -> appcodeVersion
        containsCLion -> clionVersion
        else -> rootProject.extra["versions.cidrPlatform"] as String
    }
}

fun cidrProductFriendlyVersion(productName: String, productVersion: String): String {
    val productBranch = ijProductBranch(productVersion)
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

fun defaultCidrPluginZipPath(cidrProductVersionFull: String, productName: String = "kotlin"): File =
        artifactsForCidrDir.resolve("$productName-plugin-$cidrProductVersionFull.zip").canonicalFile

fun cidrCustomPluginRepoUrl(repoUrlPropertyName: String, cidrPluginZipPath: File): URL =
        findProperty(repoUrlPropertyName)?.let {
            with (it.toString()) { URL(if (endsWith('/')) this else "$this/") }
        } ?: cidrPluginZipPath.parentFile.toURI().toURL()

fun Any?.toBoolean(): Boolean = parseBoolean(this?.toString())
