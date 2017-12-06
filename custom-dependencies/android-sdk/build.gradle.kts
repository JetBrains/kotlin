
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

data class LocMap(val name: String, val ver: String, val dest: String, val suffix: String,
                  val additionalConfig: Configuration? = null, val dirLevelsToSkit: Int = 0, val ext: String = "zip",
                  val filter: CopySpec.() -> Unit = {})

val toolsOs = when {
    OperatingSystem.current().isWindows -> "windows"
    OperatingSystem.current().isMacOsX -> "macosx"
    OperatingSystem.current().isLinux -> "linux"
    else -> {
        logger.error("Unknown operating system for android tools: ${OperatingSystem.current().name}")
        ""
    }
}

val sdkLocMaps = listOf(
        LocMap("platform", "26_r02", "platforms/android-26", "", androidPlatform, 1),
        LocMap("android_m2repository", "r44", "extras/android", ""),
        LocMap("platform-tools", "r25.0.3", "", toolsOs),
        LocMap("tools", "r24.3.4", "", toolsOs),
        LocMap("build-tools", "r23.0.1", "build-tools/23.0.1", toolsOs, buildTools, 1))

val prepareSdk by task<DefaultTask> {
    outputs.dir(sdkDestDir)
    doLast {}
}

fun LocMap.toDependency(): String =
        "google:$name:$ver${suffix?.takeIf{ it.isNotEmpty() }?.let { ":$it" } ?: ""}@$ext"

sdkLocMaps.forEach { locMap ->
    val id = "${locMap.name}_${locMap.ver}"
    val cfg = configurations.create(id)
    val dependency = locMap.toDependency()
    dependencies.add(cfg.name, dependency)

    val unzipTask = task("unzip_$id") {
        dependsOn(cfg)
        inputs.files(cfg)
        val targetDir = file("$sdkDestDir/${locMap.dest}")
        val targetFlagFile = File(targetDir, "$id.prepared")
        outputs.files(targetFlagFile)
        outputs.upToDateWhen { targetFlagFile.exists() } // TODO: consider more precise check, e.g. hash-based
        doFirst {
            project.copy {
                when (locMap.ext) {
                    "zip" -> from(zipTree(cfg.singleFile))
                    "tar.gz" -> from(tarTree(resources.gzip(cfg.singleFile)))
                    else -> throw GradleException("Don't know how to handle the extension \"${locMap.ext}\"")
                }
                locMap.filter.invoke(this)
                if (locMap.dirLevelsToSkit > 0) {
                    eachFile {
                        path = path.split("/").drop(locMap.dirLevelsToSkit).joinToString("/")
                        if (path.isBlank()) {
                            exclude()
                        }
                    }
                }
                into(targetDir)
            }
            
            targetFlagFile.writeText("prepared")
        }
    }
    prepareSdk.dependsOn(unzipTask)

    locMap.additionalConfig?.also {
        dependencies.add(it.name, dependency)
    }
}

val clean by task<Delete> {
    delete(buildDir)
}

val extractAndroidJar by tasks.creating {
    dependsOn(androidPlatform)
    inputs.files(androidPlatform)
    val targetFile = File(libsDestDir, "android.jar")
    outputs.files(targetFile)
    outputs.upToDateWhen { targetFile.exists() } // TODO: consider more precise check, e.g. hash-based
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

