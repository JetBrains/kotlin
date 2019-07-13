import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer

plugins {
    base
    id("com.github.jk1.tcdeps") version "0.19"
}

rootProject.apply {
    from(project.file("../../../gradle/kotlinUltimateProperties.gradle.kts"))
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

val clionUnscrambledJarArtifact: String by rootProject.extra
val clionUnscrambledJarDir: File by rootProject.extra
val clionPlatformDepsOrJavaPluginArtifact: String by rootProject.extra
val clionPlatformDepsOrJavaPluginDir: File by rootProject.extra

val appcodeUnscrambledJarArtifact: String by rootProject.extra
val appcodeUnscrambledJarDir: File by rootProject.extra
val appcodePlatformDepsOrJavaPluginArtifact: String by rootProject.extra
val appcodePlatformDepsOrJavaPluginDir: File by rootProject.extra

val clionUnscrambledJar: Configuration by configurations.creating
val clionPlatformDepsZip: Configuration by configurations.creating

val appcodeUnscrambledJar: Configuration by configurations.creating
val appcodePlatformDepsZip: Configuration by configurations.creating

dependencies {
    clionUnscrambledJar(tc(clionUnscrambledJarArtifact))
    clionPlatformDepsZip(tc(clionPlatformDepsOrJavaPluginArtifact))

    appcodeUnscrambledJar(tc(appcodeUnscrambledJarArtifact))
    appcodePlatformDepsZip(tc(appcodePlatformDepsOrJavaPluginArtifact))
}

val downloadCLionUnscrambledJar: Task by downloading(clionUnscrambledJar, clionUnscrambledJarDir)
val downloadCLionPlatformDeps: Task by downloading(
        clionPlatformDepsZip,
        clionPlatformDepsOrJavaPluginDir,
        pathRemap = { it.substringAfterLast('/') }
) { zipTree(it.singleFile) }

val downloadAppCodeUnscrambledJar: Task by downloading(appcodeUnscrambledJar, appcodeUnscrambledJarDir)
val downloadAppCodePlatformDeps: Task by downloading(
        appcodePlatformDepsZip,
        appcodePlatformDepsOrJavaPluginDir,
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
    // don't re-check status of the artifact at the remote server if the artifact is already downloaded
    val isUpToDate = targetDir.isDirectory && targetDir.walkTopDown().firstOrNull { !it.isDirectory } != null
    outputs.upToDateWhen { isUpToDate }

    if (!isUpToDate) {
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
}
