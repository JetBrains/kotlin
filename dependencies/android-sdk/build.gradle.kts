import org.gradle.internal.os.OperatingSystem
import java.net.URI
import java.io.File

repositories {
    ivy {
        url = URI("https://dl.google.com/android/repository")
        patternLayout {
            artifact("[artifact]-[revision].[ext]")
            artifact("[artifact]_[revision](-[classifier]).[ext]")
            artifact("[artifact]_[revision](_[classifier]).[ext]")
        }
        metadataSources {
            artifact()
        }
    }
    ivy {
        url = URI("https://dl.google.com/android/repository/sys-img/android")
        patternLayout {
            artifact("[artifact]-[revision](_[classifier]).[ext]")
        }
        metadataSources {
            artifact()
        }
    }
}

// Repo content: https://dl.google.com/android/repository/repository2-1.xml
val platformToolsVersion = "r36.0.0"
val commandLineToolsVersion = "13114758" /*19.0*/
val emulatorVersion = "14433334" // v36.4.1

dependencies {
    for (os in listOf("linux", "win", "darwin")) {
        implicitDependencies("google:platform-tools:$platformToolsVersion:$os@zip")
    }

    for (os in listOf("linux", "win", "mac")) {
        implicitDependencies("google:commandlinetools-$os:${commandLineToolsVersion}_latest@zip")
    }

    for (os in listOf("linux", "windows")) {
        implicitDependencies("google:emulator-${os}_x64:$emulatorVersion@zip")
    }

    implicitDependencies("google:emulator-darwin_aarch64:$emulatorVersion@zip")
}

val androidSdk by configurations.creating
val androidJar by configurations.creating
val androidEmulator by configurations.creating

val sdkDestDirName = "androidSdk"

val toolsOs = when {
    OperatingSystem.current().isWindows -> "windows"
    OperatingSystem.current().isMacOsX -> "macosx"
    OperatingSystem.current().isLinux -> "linux"
    else -> {
        logger.error("Unknown operating system for android tools: ${OperatingSystem.current().name}")
        ""
    }
}

val toolsOsShort = when {
    OperatingSystem.current().isWindows -> "win"
    OperatingSystem.current().isMacOsX -> "mac"
    OperatingSystem.current().isLinux -> "linux"
    else -> {
        logger.error("Unknown operating system for android tools: ${OperatingSystem.current().name}")
        ""
    }
}

val toolsOsDarwin = when {
    OperatingSystem.current().isWindows -> "win"
    OperatingSystem.current().isMacOsX -> "darwin"
    OperatingSystem.current().isLinux -> "linux"
    else -> {
        logger.error("Unknown operating system for android tools: ${OperatingSystem.current().name}")
        ""
    }
}

val toolsOsDarwinArch = when {
    OperatingSystem.current().isWindows -> "windows_x64"
    OperatingSystem.current().isMacOsX -> "darwin_aarch64"
    OperatingSystem.current().isLinux -> "linux_x64"
    else -> {
        logger.error("Unknown operating system for android tools: ${OperatingSystem.current().name}")
        ""
    }
}

val preparePlatform by task<DefaultTask> {
    doLast {}
}

val prepareSdk by task<DefaultTask> {
    doLast {}
    dependsOn(preparePlatform)
}

val prepareEmulator by task<DefaultTask> {
    doLast {}
    dependsOn(prepareSdk)
}

interface Injected {
    @get:Inject val fs: FileSystemOperations
    @get:Inject val archiveOperations: ArchiveOperations
}

fun unzipSdkTask(
    sdkName: String, sdkVer: String, destinationSubdir: String, coordinatesSuffix: String,
    additionalConfig: Configuration? = null, dirLevelsToSkipOnUnzip: Int = 0, ext: String = "zip",
    prepareTask: TaskProvider<DefaultTask> = prepareSdk,
    unzipFilter: CopySpec.() -> Unit = {},
    postProcess: (File) -> Unit = {}
): TaskProvider<Task> {
    val id = "${sdkName}_$sdkVer"
    val createdCfg = configurations.create(id)
    val dependency = "google:$sdkName:$sdkVer${coordinatesSuffix.takeIf { it.isNotEmpty() }?.let { ":$it" } ?: ""}@$ext"
    dependencies.add(createdCfg.name, dependency)

    val unzipTask = tasks.register("unzip_$id") {
        val cfg = project.configurations.getByName(id)
        dependsOn(cfg)
        inputs.files(cfg)
        val targetDir = project.layout.buildDirectory.dir("$sdkDestDirName/$destinationSubdir")
        outputs.dirs(targetDir)
        val injected = project.objects.newInstance<Injected>()
        val fs = injected.fs
        val archiveOperations = injected.archiveOperations
        val files = cfg.incoming.files
        doFirst {
            val file = files.singleFile
            fs.copy {
                when (ext) {
                    "zip" -> from(archiveOperations.zipTree(file))
                    "tar.gz" -> from(archiveOperations.tarTree(project.resources.gzip(file)))
                    else -> throw GradleException("Don't know how to handle the extension \"$ext\"")
                }
                unzipFilter.invoke(this)
                if (dirLevelsToSkipOnUnzip > 0) {
                    eachFile {
                        path = path.split("/").drop(dirLevelsToSkipOnUnzip).joinToString("/")
                        if (path.isBlank()) {
                            exclude()
                        }
                    }
                }
                into(targetDir)
            }
            postProcess(targetDir.get().asFile)
        }
    }
    prepareTask.configure {
        dependsOn(unzipTask)
    }

    additionalConfig?.also {
        dependencies.add(it.name, dependency)
    }

    return unzipTask
}

fun androidPlatform(version: String): TaskProvider<Task> {
    val artifactId = if (version.startsWith("22_")) "android" else "platform"
    return unzipSdkTask(
        sdkName = artifactId,
        sdkVer = version,
        destinationSubdir = "platforms/android-${version.substringBefore("_").substringBefore("-")}",
        coordinatesSuffix = "",
        additionalConfig = configurations.implicitDependencies.get(),
        dirLevelsToSkipOnUnzip = 1,
        prepareTask = preparePlatform
    )
}

fun androidBuildTools(version: String): TaskProvider<Task> {
    val revision = when (version) {
        "34.0.0" -> "r34"
        "35.0.0" -> "r35"
        "36.0.0" -> "r36"
        else -> "r$version"
    }

    @Suppress("LocalVariableName")
    val buildTools_30_0_3_artifactId = mapOf(
        "windows" to "91936d4ee3ccc839f0addd53c9ebf087b1e39251.build-tools",
        "macosx" to "f6d24b187cc6bd534c6c37604205171784ac5621.build-tools"
    ).withDefault { "build-tools" }

    listOf("linux", "windows", "macosx").forEach {
        dependencies {
            val artifactId = if (version == "30.0.3")
                buildTools_30_0_3_artifactId.getValue(it)
            else
                "build-tools"
            implicitDependencies("google:$artifactId:$revision:$it@zip")
        }
    }

    val artifactId = if (version == "30.0.3")
        buildTools_30_0_3_artifactId.getValue(toolsOs)
    else
        "build-tools"

    return unzipSdkTask(
        sdkName = artifactId,
        sdkVer = revision,
        destinationSubdir = "build-tools/$version",
        coordinatesSuffix = toolsOs,
        dirLevelsToSkipOnUnzip = 1
    )
}

androidPlatform("22_r02")
androidPlatform("23_r03")
androidPlatform("24_r02")
androidPlatform("26_r02")
androidPlatform("27_r03")
androidPlatform("28_r06")
androidPlatform("30_r03")
androidPlatform("31_r01")
androidPlatform("33_r02")
androidPlatform("34-ext7_r02")

androidBuildTools("29.0.3")
androidBuildTools("30.0.3")
androidBuildTools("33.0.1")
androidBuildTools("34.0.0")
androidBuildTools("35.0.0")
androidBuildTools("36.0.0")

// SDK artifacts are unpacked manually instead of using sdkmanager (recommended way),
// so required package metadata (package.xml) is missing. Write the metadata manually.
unzipSdkTask("emulator-$toolsOsDarwinArch", emulatorVersion, "", "") { emulatorDir ->
    emulatorDir.resolve("emulator/package.xml").writeText(
        """
        |<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        |<ns2:repository xmlns:ns2="http://schemas.android.com/repository/android/common/02"
        |               xmlns:ns3="http://schemas.android.com/repository/android/common/01"
        |               xmlns:ns4="http://schemas.android.com/repository/android/generic/01"
        |               xmlns:ns5="http://schemas.android.com/repository/android/generic/02"
        |               xmlns:ns6="http://schemas.android.com/sdk/android/repo/repository2/01"
        |               xmlns:ns7="http://schemas.android.com/sdk/android/repo/repository2/02"
        |               xmlns:ns8="http://schemas.android.com/sdk/android/repo/repository2/03"
        |               xmlns:ns9="http://schemas.android.com/sdk/android/repo/addon2/01"
        |               xmlns:ns10="http://schemas.android.com/sdk/android/repo/addon2/02"
        |               xmlns:ns11="http://schemas.android.com/sdk/android/repo/addon2/03"
        |               xmlns:ns12="http://schemas.android.com/sdk/android/repo/sys-img2/04"
        |               xmlns:ns13="http://schemas.android.com/sdk/android/repo/sys-img2/03"
        |               xmlns:ns14="http://schemas.android.com/sdk/android/repo/sys-img2/02"
        |               xmlns:ns15="http://schemas.android.com/sdk/android/repo/sys-img2/01">
        |  <license id="android-sdk-license" type="text">See android-sdk-license in SDK licenses directory.</license>
        |  <localPackage path="emulator" obsolete="false">
        |    <type-details xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:type="ns5:genericDetailsType"/>
        |    <revision>
        |      <major>36</major>
        |      <minor>4</minor>
        |      <micro>1</micro>
        |    </revision>
        |    <display-name>Android Emulator</display-name>
        |    <uses-license ref="android-sdk-license"/>
        |  </localPackage>
        |</ns2:repository>
        """.trimMargin()
    )
}
unzipSdkTask("arm64-v8a", "26", "system-images/android-26/default", "r02", prepareTask = prepareEmulator) { systemImageBaseDir ->
    systemImageBaseDir.resolve("arm64-v8a/package.xml").writeText(
        """
        |<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        |<ns2:repository xmlns:ns2="http://schemas.android.com/repository/android/common/02"
        |               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        |               xmlns:ns12="http://schemas.android.com/sdk/android/repo/sys-img2/04">
        |  <license id="android-sdk-license" type="text">See android-sdk-license in SDK licenses directory.</license>
        |  <localPackage path="system-images;android-26;default;arm64-v8a" obsolete="false">
        |    <type-details xsi:type="ns12:sysImgDetailsType">
        |      <api-level>26</api-level>
        |      <base-extension>true</base-extension>
        |      <tag>
        |        <id>default</id>
        |        <display>Default Android System Image</display>
        |      </tag>
        |      <abi>arm64-v8a</abi>
        |      <abis>arm64-v8a</abis>
        |    </type-details>
        |    <revision>
        |      <major>2</major>
        |    </revision>
        |    <display-name>ARM 64 v8a System Image</display-name>
        |    <uses-license ref="android-sdk-license"/>
        |  </localPackage>
        |</ns2:repository>
        """.trimMargin()
    )
}
unzipSdkTask("x86_64", "26", "system-images/android-26/default", "r01", prepareTask = prepareEmulator) { systemImageBaseDir ->
    systemImageBaseDir.resolve("package.xml").delete()
    systemImageBaseDir.resolve("x86_64/package.xml").writeText(
        """
        |<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        |<ns2:repository xmlns:ns2="http://schemas.android.com/repository/android/common/02"
        |               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        |               xmlns:ns12="http://schemas.android.com/sdk/android/repo/sys-img2/04">
        |  <license id="android-sdk-license" type="text">See android-sdk-license in SDK licenses directory.</license>
        |  <localPackage path="system-images;android-26;default;x86_64" obsolete="false">
        |    <type-details xsi:type="ns12:sysImgDetailsType">
        |      <api-level>26</api-level>
        |      <base-extension>true</base-extension>
        |      <tag>
        |        <id>default</id>
        |        <display>Default Android System Image</display>
        |      </tag>
        |      <abi>x86_64</abi>
        |      <abis>x86_64</abis>
        |    </type-details>
        |    <revision>
        |      <major>1</major>
        |    </revision>
        |    <display-name>Intel x86_64 Atom System Image</display-name>
        |    <uses-license ref="android-sdk-license"/>
        |  </localPackage>
        |</ns2:repository>
        """.trimMargin()
    )
}
unzipSdkTask("android_m2repository", "r44", "extras/android", "")
unzipSdkTask("platform-tools", platformToolsVersion, "", toolsOsDarwin)
unzipSdkTask("commandlinetools-$toolsOsShort", "${commandLineToolsVersion}_latest", "", "")

val clean by task<Delete> {
    delete(layout.buildDirectory)
}

artifacts.add(androidSdk.name, layout.buildDirectory.dir(sdkDestDirName)) {
    builtBy(prepareSdk)
}

artifacts.add(androidJar.name, layout.buildDirectory.file("$sdkDestDirName/platforms/android-26/android.jar")) {
    builtBy(preparePlatform)
}

artifacts.add(androidEmulator.name, layout.buildDirectory.dir(sdkDestDirName)) {
    builtBy(prepareEmulator)
}
