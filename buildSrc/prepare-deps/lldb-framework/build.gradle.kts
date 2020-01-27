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

if (rootProject.extra.has("lldbFrameworkRepo")) {
    val lldbFrameworkVersion: String by rootProject.extra
    val lldbFrameworkRepo: String by rootProject.extra
    val lldbFrameworkDir: File by rootProject.extra

    val lldbFrameworkZip: Configuration by configurations.creating

    dependencies {
        lldbFrameworkZip(tc("$lldbFrameworkRepo:$lldbFrameworkVersion:LLDB.framework.tar.gz"))
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
