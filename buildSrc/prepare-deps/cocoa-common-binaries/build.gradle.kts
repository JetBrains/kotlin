
plugins {
    base
}

rootProject.apply {
    from(project.file("../../../gradle/kotlinUltimateProperties.gradle.kts"))
    from(project.file("../../../gradle/kotlinUltimateTools.gradle.kts"))
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

val clionVersion: String by rootProject.extra
val clionCocoaCommonModule: String by rootProject.extra
val clionCocoaCommonArtifacts: List<String> by rootProject.extra
val clionCocoaCommonBinariesDir: File by rootProject.extra
val clionCocoaCommonBinaries: Configuration by configurations.creating

val ultimateTools: Map<String, Any> by rootProject.extensions
val handleSymlink: (FileCopyDetails, File) -> Boolean by ultimateTools

dependencies {
    clionCocoaCommonArtifacts.forEach {
        clionCocoaCommonBinaries(clionCocoaCommonModule) {
            artifact {
                name = it.substringBeforeLast('.')
                extension = it.substringAfterLast('.')
                type = it.substringAfterLast('.')
            }
        }
    }
}

val downloadCLionCocoaCommonBinaries: Task by downloading(
    clionCocoaCommonBinaries,
    clionCocoaCommonBinariesDir,
    pathRemap = { it.replace("-$clionVersion", "") }
) { config, tempDir ->
    config.map {
        when {
            it.name.endsWith(".tar") -> {
                val dir = File(tempDir, it.name)
                dir.mkdir()
                project.exec {
                    setCommandLine("tar", "xf", it.absolutePath, "-C", dir.absolutePath)
                }
                files(dir.absolutePath)
            }
            else -> it
        }
    }
}

tasks["build"].dependsOn(downloadCLionCocoaCommonBinaries)

fun Project.downloading(
    sourceConfiguration: Configuration,
    targetDir: File,
    pathRemap: (String) -> String = { it },
    extractor: (Configuration, File) -> Any = { it, _ -> it }
) = tasks.creating {
    // don't re-check status of the artifact at the remote server if the artifact is already downloaded
    val isUpToDate = targetDir.isDirectory && targetDir.walkTopDown().firstOrNull { !it.isDirectory } != null
    outputs.upToDateWhen { isUpToDate }

    if (!isUpToDate) {
        doFirst {
            copy {
                from(extractor(sourceConfiguration, temporaryDir))
                into(targetDir)
                includeEmptyDirs = false
                duplicatesStrategy = DuplicatesStrategy.FAIL
                eachFile {
                    if (!handleSymlink(this, targetDir)) {
                        path = pathRemap(path)
                    }
                }
            }
        }
    }
}
