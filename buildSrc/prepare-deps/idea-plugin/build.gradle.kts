import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc

plugins {
    base
    id("com.github.jk1.tcdeps") version "0.18"
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

val ideaPlugin by configurations.creating

dependencies {
    ideaPlugin(tc("$ideaPluginForCidrRepo:$ideaPluginForCidrVersion:kotlin-plugin-$ideaPluginForCidrBuildNumber-$ideaPluginForCidrIde.zip"))
}

val downloadIdeaPlugin by downloading(ideaPlugin, file(ideaPluginForCidrDir).parent,  extract = true)

tasks["build"].dependsOn(downloadIdeaPlugin)

fun Project.downloading(
        sourceConfiguration: Configuration,
        targetDir: Any,
        extract: Boolean = false
) = tasks.creating {
    dependsOn(sourceConfiguration)
    inputs.files(sourceConfiguration)
    outputs.dir(targetDir)

    doFirst {
        copy {
            if (extract) from(zipTree(sourceConfiguration.singleFile)) else from(sourceConfiguration)
            into(targetDir)
        }
    }
}
