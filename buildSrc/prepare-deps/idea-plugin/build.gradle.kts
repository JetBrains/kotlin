import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc

plugins {
    base
    id("com.github.jk1.tcdeps") version "0.18"
}

rootProject.apply {
    from(project.file("../../../gradle/cidrPluginProperties.gradle.kts"))
}

repositories {
    teamcityServer {
        setUrl("https://teamcity.jetbrains.com")
        credentials {
            username = "guest"
            password = "guest"
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
    ideaPlugin(tc("$ideaPluginForCidrRepo:$ideaPluginForCidrVersion:kotlin-plugin-$ideaPluginForCidrBuildNumber-$ideaPluginForCidrIde.zip"))
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
