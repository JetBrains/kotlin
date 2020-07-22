plugins {
    base
}

rootProject.apply {
    from(project.file("../../../gradle/kotlinUltimateProperties.gradle.kts"))
}

repositories {
    ivy {
        url = uri("https://buildserver.labs.intellij.net/guestAuth/repository/download")
        patternLayout {
            ivy("[module]/[revision]/teamcity-ivy.xml")
            artifact("[module]/[revision]/[artifact](.[ext])")
        }
    }
}

val clionUnscrambledJarArtifact: String by rootProject.extra
val clionUnscrambledJarDir: File by rootProject.extra

val appcodeUnscrambledJarArtifact: String by rootProject.extra
val appcodeUnscrambledJarDir: File by rootProject.extra

val clionUnscrambledJar: Configuration by configurations.creating
val appcodeUnscrambledJar: Configuration by configurations.creating

dependencies {
    clionUnscrambledJar(clionUnscrambledJarArtifact.substringBeforeLast(':')) {
        val jar = clionUnscrambledJarArtifact.substringAfterLast(':')
        artifact {
            name = jar.substringBeforeLast(".")
            type = "jar"
            extension = "jar"
        }
    }

    appcodeUnscrambledJar(appcodeUnscrambledJarArtifact.substringBeforeLast(':')) {
        val jar = appcodeUnscrambledJarArtifact.substringAfterLast(':')
        artifact {
            name = jar.substringBeforeLast(".")
            type = "jar"
            extension = "jar"
        }
    }
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
