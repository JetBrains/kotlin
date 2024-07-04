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

val buildTools_30_0_3_artifactId = mapOf(
    "windows" to "91936d4ee3ccc839f0addd53c9ebf087b1e39251.build-tools",
    "macosx" to "f6d24b187cc6bd534c6c37604205171784ac5621.build-tools"
).withDefault { "build-tools" }

dependencies {
    implicitDependencies("google:platform:23_r03@zip")
    implicitDependencies("google:platform:26_r02@zip")
    implicitDependencies("google:platform:33_r02@zip")
    implicitDependencies("google:platform:34-ext7_r02@zip")

    implicitDependencies("google:build-tools:r29.0.3:linux@zip")
    implicitDependencies("google:build-tools:r29.0.3:windows@zip")
    implicitDependencies("google:build-tools:r29.0.3:macosx@zip")

    implicitDependencies("google:${buildTools_30_0_3_artifactId.getValue("linux")}:r30.0.3:linux@zip")
    implicitDependencies("google:${buildTools_30_0_3_artifactId.getValue("windows")}:r30.0.3:windows@zip")
    implicitDependencies("google:${buildTools_30_0_3_artifactId.getValue("macosx")}:r30.0.3:macosx@zip")

    implicitDependencies("google:build-tools:r33.0.1:linux@zip")
    implicitDependencies("google:build-tools:r33.0.1:windows@zip")
    implicitDependencies("google:build-tools:r33.0.1:macosx@zip")

    implicitDependencies("google:build-tools:r34:linux@zip")
    implicitDependencies("google:build-tools:r34:windows@zip")
    implicitDependencies("google:build-tools:r34:macosx@zip")

    implicitDependencies("google:platform-tools:$platformToolsVersion:linux@zip")
    implicitDependencies("google:platform-tools:$platformToolsVersion:windows@zip")
    implicitDependencies("google:platform-tools:$platformToolsVersion:darwin@zip")

    implicitDependencies("google:sdk-tools-linux:$sdkToolsVersion@zip")
    implicitDependencies("google:sdk-tools-windows:$sdkToolsVersion@zip")
    implicitDependencies("google:sdk-tools-darwin:$sdkToolsVersion@zip")

    implicitDependencies("google:emulator-linux:$emulatorVersion@zip")
    implicitDependencies("google:emulator-windows:$emulatorVersion@zip")
    implicitDependencies("google:emulator-darwin:$emulatorVersion@zip")
}

val androidSdk by configurations.creating
val androidJar by configurations.creating
val androidPlatform by configurations.creating
val buildTools by configurations.creating
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

unzipSdkTask("android", "22_r02", "platforms/android-22", "", androidPlatform, 1, prepareTask = preparePlatform)
unzipSdkTask("platform", "23_r03", "platforms/android-23", "", androidPlatform, 1, prepareTask = preparePlatform)
unzipSdkTask("platform", "24_r02", "platforms/android-24", "", androidPlatform, 1, prepareTask = preparePlatform)
unzipSdkTask("platform", "26_r02", "platforms/android-26", "", androidPlatform, 1, prepareTask = preparePlatform)
unzipSdkTask("platform", "27_r03", "platforms/android-27", "", androidPlatform, 1, prepareTask = preparePlatform)
unzipSdkTask("platform", "28_r06", "platforms/android-28", "", androidPlatform, 1, prepareTask = preparePlatform)
unzipSdkTask("platform", "30_r03", "platforms/android-30", "", androidPlatform, 1, prepareTask = preparePlatform)
unzipSdkTask("platform", "31_r01", "platforms/android-31", "", androidPlatform, 1, prepareTask = preparePlatform)
unzipSdkTask("platform", "33_r02", "platforms/android-33", "", androidPlatform, 1, prepareTask = preparePlatform)
unzipSdkTask("platform", "34-ext7_r02", "platforms/android-34", "", androidPlatform, 1, prepareTask = preparePlatform)

unzipSdkTask("build-tools", "r29.0.3", "build-tools/29.0.3", toolsOs, buildTools, 1)
unzipSdkTask(buildTools_30_0_3_artifactId.getValue(toolsOs), "r30.0.3", "build-tools/30.0.3", toolsOs, buildTools, 1)
unzipSdkTask("build-tools", "r33.0.1", "build-tools/33.0.1", toolsOs, buildTools, 1)
unzipSdkTask("build-tools", "r34", "build-tools/34.0.0", toolsOs, buildTools, 1)

unzipSdkTask("android_m2repository", "r44", "extras/android", "")
unzipSdkTask("platform-tools", platformToolsVersion, "", toolsOsDarwin)
unzipSdkTask("sdk-tools-$toolsOsDarwin", sdkToolsVersion, "", "")
unzipSdkTask("emulator-$toolsOsDarwin", emulatorVersion, "", "", prepareTask = prepareEmulator)
unzipSdkTask("armeabi-v7a", "19", "system-images/android-19/default","r05", prepareTask = prepareEmulator)
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