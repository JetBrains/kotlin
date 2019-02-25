import com.github.jk1.tcdeps.KotlinScriptDslAdapter.teamcityServer
import com.github.jk1.tcdeps.KotlinScriptDslAdapter.tc

plugins {
    base
    id("com.github.jk1.tcdeps") version "0.18"
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

val clionRepo: String by rootProject.extra
val clionVersion: String by rootProject.extra
val clionDir: File by rootProject.extra

val clion by configurations.creating

dependencies {
    clion(tc("$clionRepo:$clionVersion:CLion-$clionVersion.zip"))
}

val downloadCLion by downloading(clion, clionDir, extract = true)

tasks["build"].dependsOn(downloadCLion)

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
