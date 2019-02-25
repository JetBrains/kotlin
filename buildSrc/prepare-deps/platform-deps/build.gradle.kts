import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer

plugins {
    base
    id("com.github.jk1.tcdeps") version "0.18"
}

repositories {
    teamcityServer {
        setUrl("https://buildserver.labs.intellij.net")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

val clionRepo: String by rootProject.extra
val clionVersion: String by rootProject.extra
val clionPlatformDepsDir: File by rootProject.extra
val clionUnscrambledJarDir: File by rootProject.extra

val appcodeRepo: String by rootProject.extra
val appcodeVersion: String by rootProject.extra
val appcodePlatformDepsDir: File by rootProject.extra
val appcodeUnscrambledJarDir: File by rootProject.extra

val clionPlatformDepsZip by configurations.creating
val clionUnscrambledJar by configurations.creating

val appcodePlatformDepsZip by configurations.creating
val appcodeUnscrambledJar by configurations.creating

dependencies {
    clionPlatformDepsZip(tc("$clionRepo:$clionVersion:CL-plugins/kotlinNative-platformDeps-$clionVersion.zip"))
    clionUnscrambledJar(tc("$clionRepo:$clionVersion:unscrambled/clion.jar"))

    appcodePlatformDepsZip(tc("$appcodeRepo:$appcodeVersion:OC-plugins/kotlinNative-platformDeps-$appcodeVersion.zip"))
    appcodeUnscrambledJar(tc("$appcodeRepo:$appcodeVersion:unscrambled/appcode.jar"))
}

val downloadCLionPlatformDeps by downloading(clionPlatformDepsZip, clionPlatformDepsDir, extract = true)
val downloadCLionUnscrambledJar by downloading(clionUnscrambledJar, clionUnscrambledJarDir)

val downloadAppCodePlatformDeps by downloading(appcodePlatformDepsZip, appcodePlatformDepsDir, extract = true)
val downloadAppCodeUnscrambledJar by downloading(appcodeUnscrambledJar, appcodeUnscrambledJarDir)

tasks["build"].dependsOn(
        downloadCLionPlatformDeps,
        downloadCLionUnscrambledJar,
        downloadAppCodePlatformDeps,
        downloadAppCodeUnscrambledJar
)

fun Project.downloading(
        sourceConfiguration: Configuration,
        targetDir: Any,
        extract: Boolean = false
) = tasks.creating {
    dependsOn(sourceConfiguration)
    inputs.files(sourceConfiguration)
    outputs.dir(targetDir)

    doFirst {
        copy {
            if (extract) from(zipTree(sourceConfiguration.singleFile)) else from(sourceConfiguration)
            into(targetDir)
        }
    }
}
