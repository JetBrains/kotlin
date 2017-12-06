
import java.io.File
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.tasks.Jar

val toolsOs by lazy {
    when {
        OperatingSystem.current().isWindows -> "windows"
        OperatingSystem.current().isMacOsX -> "macosx"
        OperatingSystem.current().isLinux -> "linux"
        else -> {
            logger.error("Unknown operating system for android tools: ${OperatingSystem.current().name}")
            ""
        }
    }
}

val buildToolsVersion = "r23.0.1"

repositories {
    ivy {
        artifactPattern("https://dl-ssl.google.com/android/repository/[artifact]_[revision](-[classifier]).[ext]")
        artifactPattern("https://android.googlesource.com/platform/dalvik/+archive/android-5.0.0_r2/[artifact].[ext]")
    }
}

val dxRepoDir = File(buildDir, "libs")

val buildToolsZip by configurations.creating
val dxSourcesTar by configurations.creating

dependencies {
    buildToolsZip("google:build-tools:$buildToolsVersion:$toolsOs@zip")
    dxSourcesTar("google:dx:0@tar.gz")
}

val unzipDxJar by tasks.creating {
    dependsOn(buildToolsZip)
    inputs.files(buildToolsZip)
    outputs.files(File(dxRepoDir, "dx.jar"))
    doFirst {
        project.copy {
            from(zipTree(buildToolsZip.singleFile).files)
            include("**/dx.jar")
            into(dxRepoDir)
        }
    }
}

val dxSourcesTargetDir = File(buildDir, "dx_src")

val untarDxSources by tasks.creating {
    dependsOn(dxSourcesTar)
    inputs.files(dxSourcesTar)
    outputs.dir(dxSourcesTargetDir)
    doFirst {
        project.copy {
            from(tarTree(dxSourcesTar.singleFile))
            include("src/**")
            includeEmptyDirs = false
            into(dxSourcesTargetDir)
        }
    }
}

val prepareDxSourcesJar by tasks.creating(Jar::class) {
    dependsOn(untarDxSources)
    from("$dxSourcesTargetDir/src")
    destinationDir = dxRepoDir
    baseName = "dx"
    classifier = "sources"
}

val build by tasks.creating {
    dependsOn(unzipDxJar, prepareDxSourcesJar)
}

val clean by tasks.creating(Delete::class) {
    delete(buildDir)
}
