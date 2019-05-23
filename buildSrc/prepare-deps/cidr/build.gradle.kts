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
        setUrl("https://buildserver.labs.intellij.net")
        credentials {
            username = "guest"
            password = "guest"
        }
    }
}

val cidrIdeArtifact: String by rootProject.extra
val cidrIdeDir: File by rootProject.extra

val cidrIde: Configuration by configurations.creating

dependencies {
    cidrIde(tc(cidrIdeArtifact))
}

val downloadCidrIde: Task by downloading(
    cidrIde,
    cidrIdeDir,
    pathRemap = { it.substringAfter('/').substringAfter("Contents/") }
) {
    val file = it.singleFile
    if (file.name.endsWith(".sit")) {
        zipTree(file).matching {
            include("*/Contents/lib/*.jar")
            exclude("*/Contents/lib/clion*.jar")
            exclude("*/Contents/lib/appcode*.jar")
            exclude("*/Contents/lib/kotlin*.jar")
            include("*/Contents/plugins/cidr-*/lib/*.jar")
            include("*/Contents/plugins/gradle/lib/*.jar")
        }
    }
    else {
        tarTree(file).matching {
            include("*/lib/*.jar")
            exclude("*/lib/clion*.jar")
            exclude("*/lib/appcode*.jar")
            exclude("*/lib/kotlin*.jar")
            include("*/plugins/cidr-*/lib/*.jar")
            include("*/plugins/gradle/lib/*.jar")
        }
    }
}

tasks["build"].dependsOn(downloadCidrIde)

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
