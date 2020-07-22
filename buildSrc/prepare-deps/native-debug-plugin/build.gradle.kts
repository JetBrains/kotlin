
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

if (rootProject.extra.has("nativeDebugRepo")) {
    val nativeDebugRepo: String by rootProject.extra
    val nativeDebugVersion: String by rootProject.extra
    val nativeDebugPluginDir: File by rootProject.extra

    val nativeDebugPluginZip: Configuration by configurations.creating


    dependencies {
        var urlPath = "IU-plugins/auto-uploading/nativeDebug-plugin"

        for (version in listOf("183", "191", "192")) {
            if (nativeDebugVersion.startsWith(version)) {
                urlPath = "IU-plugins/nativeDebug-plugin"
            }
        }

        nativeDebugPluginZip("org:$nativeDebugRepo:$nativeDebugVersion") {
            artifact {
                name = urlPath
                extension = "zip"
                type = "zip"
            }
        }
    }

    val downloadNativeDebugPlugin: Task by downloading(
        nativeDebugPluginZip,
        nativeDebugPluginDir,
        pathRemap = { it.substringAfterLast('/') }
    ) { zipTree(it.singleFile) }

    tasks["build"].dependsOn(
        downloadNativeDebugPlugin
    )
}

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
