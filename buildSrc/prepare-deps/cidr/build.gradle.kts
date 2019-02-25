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

val clion: Configuration by configurations.creating

dependencies {
    clion(tc("$clionRepo:$clionVersion:CLion-$clionVersion.tar.gz"))
}

val downloadCLion: Task by downloading(
        clion,
        clionDir,
        pathRemap = { it.substringAfter('/') }
) {
    tarTree(it.singleFile).matching {
        include("*/lib/*.jar")
        exclude("*/lib/clion*.jar")
        exclude("*/lib/kotlin*.jar")
        include("*/plugins/cidr-*/lib/*.jar")
        include("*/plugins/gradle/lib/*.jar")
    }
}

tasks["build"].dependsOn(downloadCLion)

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
