import org.gradle.internal.os.OperatingSystem
import java.net.URI

repositories {
    ivy {
        url = URI("https://dl.google.com/android/repository")
        patternLayout {
            artifact("[artifact]-[revision].[ext]")
            artifact("[artifact]_[revision](-[classifier]).[ext]")
            artifact("[artifact]_[revision](-[classifier]).[ext]")
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

val platformToolsVersion = "r28.0.1"
val sdkToolsVersion = "4333796" /*26.1.1*/
val emulatorVersion = "5264690"

dependencies {
    listOf("linux", "windows", "darwin").forEach {
        implicitDependencies("google:platform-tools:$platformToolsVersion:$it@zip")
        implicitDependencies("google:sdk-tools-$it:$sdkToolsVersion@zip")
        implicitDependencies("google:emulator-$it:$emulatorVersion@zip")
    }
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

val toolsOsDarwin = when {
    OperatingSystem.current().isWindows -> "windows"
    OperatingSystem.current().isMacOsX -> "darwin"
    OperatingSystem.current().isLinux -> "linux"
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
    unzipFilter: CopySpec.() -> Unit = {}
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
        val file = cfg.singleFile
        doFirst {
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
    val revision = if (version == "34.0.0") "r34" else "r$version"

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

unzipSdkTask("android_m2repository", "r44", "extras/android", "")
unzipSdkTask("platform-tools", platformToolsVersion, "", toolsOsDarwin)
unzipSdkTask("sdk-tools-$toolsOsDarwin", sdkToolsVersion, "", "")
unzipSdkTask("emulator-$toolsOsDarwin", emulatorVersion, "", "", prepareTask = prepareEmulator)
unzipSdkTask("armeabi-v7a", "19", "system-images/android-19/default", "r05", prepareTask = prepareEmulator)
unzipSdkTask("x86", "19", "system-images/android-19/default", "r06", prepareTask = prepareEmulator)

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