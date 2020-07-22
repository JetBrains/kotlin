
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

if (rootProject.extra.has("lldbFrameworkRepo")) {
    val lldbFrameworkVersion: String by rootProject.extra
    val lldbFrameworkRepo: String by rootProject.extra
    val lldbFrameworkDir: File by rootProject.extra

    val lldbFrameworkZip: Configuration by configurations.creating

    dependencies {
        lldbFrameworkZip("org:$lldbFrameworkRepo:$lldbFrameworkVersion") {
            artifact {
                name = "LLDB.framework.tar"
                type = "gz"
                extension = "gz"
            }
        }
    }

    val downloadLldbFramework: Task by downloading(
        lldbFrameworkZip,
        lldbFrameworkDir,
        pathRemap = { it }
    ) { tarTree(it.singleFile) }

    tasks["build"].dependsOn(downloadLldbFramework)
}

fun Project.downloading(
    sourceConfiguration: Configuration,
    targetDir: File,
    pathRemap: (String) -> String,
    extractor: (Configuration) -> FileTree
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
