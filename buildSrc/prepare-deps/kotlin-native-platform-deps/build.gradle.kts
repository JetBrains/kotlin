import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer

plugins {
    base
    id("com.github.jk1.tcdeps") version "1.2"
}

rootProject.apply {
    from(project.file("../../../gradle/kotlinUltimateProperties.gradle.kts"))
}

repositories {
    teamcityServer {
        setUrl("https://buildserver.labs.intellij.net")
    }
}

val clionUnscrambledJarArtifact: String by rootProject.extra
val clionUnscrambledJarDir: File by rootProject.extra

val appcodeUnscrambledJarArtifact: String by rootProject.extra
val appcodeUnscrambledJarDir: File by rootProject.extra

val clionUnscrambledJar: Configuration by configurations.creating
val appcodeUnscrambledJar: Configuration by configurations.creating

dependencies {
    clionUnscrambledJar(tc(clionUnscrambledJarArtifact))

    appcodeUnscrambledJar(tc(appcodeUnscrambledJarArtifact))
}

val downloadCLionUnscrambledJar: Task by downloading(clionUnscrambledJar, clionUnscrambledJarDir)
val downloadAppCodeUnscrambledJar: Task by downloading(appcodeUnscrambledJar, appcodeUnscrambledJarDir)

tasks["build"].dependsOn(
        downloadCLionUnscrambledJar,
        downloadAppCodeUnscrambledJar
)

fun Project.downloading(
        sourceConfiguration: Configuration,
        targetDir: File,
        pathRemap: (String) -> String = { it },
        extractor: (Configuration) -> Any = { it }
) = tasks.creating {
    onlyIf { findProperty("skipDownloading" + sourceConfiguration.name.capitalize())?.toString()?.toBoolean() != true }
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
