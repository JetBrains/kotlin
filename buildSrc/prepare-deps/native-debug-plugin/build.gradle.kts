import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import java.io.File

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

// this module is being built for each gradle invocation
// while download should happen only for plugin construction
if (rootProject.extra.has("nativeDebugRepo") && rootProject.hasProperty("pluginVersion")) {
    val nativeDebugRepo: String by rootProject.extra
    val nativeDebugVersion: String by rootProject.extra
    val nativeDebugPluginDir: File by rootProject.extra

    val nativeDebugPluginZip: Configuration by configurations.creating


    dependencies {
        nativeDebugPluginZip(tc("$nativeDebugRepo:$nativeDebugVersion:IU-plugins/nativeDebug-plugin.zip"))
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
