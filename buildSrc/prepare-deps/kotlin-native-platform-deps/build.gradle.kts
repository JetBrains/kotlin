import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer

plugins {
    base
    id("com.github.jk1.tcdeps") version "0.18"
}

rootProject.apply {
    from(project.file("../../../gradle/cidrPluginProperties.gradle.kts"))
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

val clionUnscrambledJar: Configuration by configurations.creating
val clionPlatformDepsZip: Configuration by configurations.creating

val appcodeUnscrambledJar: Configuration by configurations.creating
val appcodePlatformDepsZip: Configuration by configurations.creating

dependencies {
    clionUnscrambledJar(tc("$clionRepo:$clionVersion:unscrambled/clion.jar"))
    clionPlatformDepsZip(tc("$clionRepo:$clionVersion:CL-plugins/kotlinNative-platformDeps-$clionVersion.zip"))

    appcodeUnscrambledJar(tc("$appcodeRepo:$appcodeVersion:unscrambled/appcode.jar"))
    appcodePlatformDepsZip(tc("$appcodeRepo:$appcodeVersion:OC-plugins/kotlinNative-platformDeps-$appcodeVersion.zip"))
}

val downloadCLionUnscrambledJar: Task by downloading(clionUnscrambledJar, clionUnscrambledJarDir)
val downloadCLionPlatformDeps: Task by downloading(
        clionPlatformDepsZip,
        clionPlatformDepsDir,
        pathRemap = { it.substringAfterLast('/') }
) { zipTree(it.singleFile) }

val downloadAppCodeUnscrambledJar: Task by downloading(appcodeUnscrambledJar, appcodeUnscrambledJarDir)
val downloadAppCodePlatformDeps: Task by downloading(
        appcodePlatformDepsZip,
        appcodePlatformDepsDir,
        pathRemap = { it.substringAfterLast('/') }
) { zipTree(it.singleFile) }

tasks["build"].dependsOn(
        downloadCLionUnscrambledJar,
        downloadCLionPlatformDeps,
        downloadAppCodeUnscrambledJar,
        downloadAppCodePlatformDeps
)

fun Project.downloading(
        sourceConfiguration: Configuration,
        targetDir: File,
        pathRemap: (String) -> String = { it },
        extractor: (Configuration) -> Any = { it }
) = tasks.creating {
    dependsOn(sourceConfiguration)
    inputs.files(sourceConfiguration)
    outputs.dir(targetDir)

    doFirst {
        copy {
            from(extractor(sourceConfiguration))
            into(targetDir)
            includeEmptyDirs = false
            duplicatesStrategy = DuplicatesStrategy.FAIL
            eachFile {
                path = pathRemap(path)
            }
        }
    }
}
