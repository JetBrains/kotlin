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

val ideaPluginForCidrRepo: String by rootProject.extra
val ideaPluginForCidrVersion: String by rootProject.extra
val ideaPluginForCidrBuildNumber: String by rootProject.extra
val ideaPluginForCidrIde: String by rootProject.extra
val ideaPluginForCidrDir: File by rootProject.extra

val ideaPlugin: Configuration by configurations.creating

dependencies {
    ideaPlugin("org:$ideaPluginForCidrRepo:$ideaPluginForCidrVersion") {
        artifact {
            name = "kotlin-plugin-$ideaPluginForCidrBuildNumber-$ideaPluginForCidrIde"
            extension = "zip"
            type = "zip"
        }
    }
}

val downloadIdeaPlugin: Task by downloading(
    ideaPlugin,
    ideaPluginForCidrDir,
    pathRemap = { it.substringAfter("Kotlin/") }
) {
    zipTree(it.singleFile).matching {
        include("Kotlin/lib/**/*.jar")
    }
}

tasks["build"].dependsOn(downloadIdeaPlugin)

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
