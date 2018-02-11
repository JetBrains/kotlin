
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.File
import org.gradle.internal.os.OperatingSystem

// TODO: consider adding dx sources (the only jar used on the compile time so far)
// e.g. from "https://android.googlesource.com/platform/dalvik/+archive/android-5.0.0_r2/dx.tar.gz"

repositories {
    ivy {
        artifactPattern("https://dl-ssl.google.com/android/repository/[artifact]-[revision].[ext]")
        artifactPattern("https://dl-ssl.google.com/android/repository/[artifact]_[revision](-[classifier]).[ext]")
        artifactPattern("https://dl.google.com/android/repository/[artifact]_[revision](-[classifier]).[ext]")
    }
}

val androidSdk by configurations.creating
val androidJar by configurations.creating
val androidPlatform by configurations.creating
val dxSources by configurations.creating
val buildTools by configurations.creating

val libsDestDir = File(buildDir, "libs")
val sdkDestDir = File(buildDir, "androidSdk")

val toolsOs = when {
    OperatingSystem.current().isWindows -> "windows"
    OperatingSystem.current().isMacOsX -> "macosx"
    OperatingSystem.current().isLinux -> "linux"
    else -> {
        logger.error("Unknown operating system for android tools: ${OperatingSystem.current().name}")
        ""
    }
}

val prepareSdk by task<DefaultTask> {
    doLast {}
}

fun unzipSdkTask(
    sdkName: String, sdkVer: String, destinationSubdir: String, coordinatesSuffix: String,
    additionalConfig: Configuration? = null, dirLevelsToSkipOnUnzip: Int = 0, ext: String = "zip",
    unzipFilter: CopySpec.() -> Unit = {}
): DefaultTask {
    val id = "${sdkName}_$sdkVer"
    val cfg = configurations.create(id)
    val dependency = "google:$sdkName:$sdkVer${coordinatesSuffix.takeIf{ it.isNotEmpty() }?.let { ":$it" } ?: ""}@$ext"
    dependencies.add(cfg.name, dependency)

    val unzipTask = task("unzip_$id") {
        dependsOn(cfg)
        inputs.files(cfg)
        val targetDir = file("$sdkDestDir/$destinationSubdir")
        outputs.dirs(targetDir)
        doFirst {
            project.copy {
                when (ext) {
                    "zip" -> from(zipTree(cfg.singleFile))
                    "tar.gz" -> from(tarTree(resources.gzip(cfg.singleFile)))
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
    prepareSdk.dependsOn(unzipTask)

    additionalConfig?.also {
        dependencies.add(it.name, dependency)
    }
    
    return unzipTask
}

unzipSdkTask("platform", "26_r02", "platforms/android-26", "", androidPlatform, 1)
unzipSdkTask("android_m2repository", "r44", "extras/android", "")
unzipSdkTask("platform-tools", "r25.0.3", "", toolsOs)
unzipSdkTask("tools", "r24.3.4", "", toolsOs)
unzipSdkTask("build-tools", "r23.0.1", "build-tools/23.0.1", toolsOs, buildTools, 1)


val clean by task<Delete> {
    delete(buildDir)
}

val extractAndroidJar by tasks.creating {
    dependsOn(androidPlatform)
    inputs.files(androidPlatform)
    val targetFile = File(libsDestDir, "android.jar")
    outputs.files(targetFile)
    doFirst {
        project.copy {
            from(zipTree(androidPlatform.singleFile).matching { include("**/android.jar") }.files.first())
            into(libsDestDir)
        }
    }
}

artifacts.add(androidSdk.name, file("$sdkDestDir")) {
    builtBy(prepareSdk)
}

artifacts.add(androidJar.name, file("$libsDestDir/android.jar")) {
    builtBy(extractAndroidJar)
}
