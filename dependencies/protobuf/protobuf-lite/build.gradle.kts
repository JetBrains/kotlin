
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

plugins {
    base
    `maven-publish`
}

val relocatedProtobuf by configurations.creating
val relocatedProtobufSources by configurations.creating

val protobufVersion: String by rootProject.extra
val protobufJarPrefix = "protobuf-$protobufVersion"
val outputJarPath = "$buildDir/libs/$protobufJarPrefix-lite.jar"

dependencies {
    relocatedProtobuf(project(":protobuf-relocated"))
}

val prepare by tasks.creating {
    inputs.files(relocatedProtobuf) // this also adds a dependency
    outputs.file(outputJarPath)
    doFirst {
        File(outputJarPath).parentFile.mkdirs()
    }
    doLast {
        val INCLUDE_START = "<include>**/"
        val INCLUDE_END = ".java</include>"
        val POM_PATH = "META-INF/maven/com.google.protobuf/protobuf-java/pom.xml"

        fun loadAllFromJar(file: File): Map<String, Pair<JarEntry, ByteArray>> {
            val result = hashMapOf<String, Pair<JarEntry, ByteArray>>()
            JarFile(file).use { jar ->
                for (jarEntry in jar.entries()) {
                    result[jarEntry.name] = Pair(jarEntry, jar.getInputStream(jarEntry).readBytes())
                }
            }
            return result
        }

        val mainJar = relocatedProtobuf.resolvedConfiguration.resolvedArtifacts.single {
            it.name == "protobuf-relocated" && it.classifier == null
        }.file

        val allFiles = loadAllFromJar(mainJar)

        val keepClasses = arrayListOf<String>()

        val pomBytes = allFiles[POM_PATH]?.second ?: error("pom.xml is not found in protobuf jar at $POM_PATH")
        val lines = String(pomBytes).lines()

        var liteProfileReached = false
        for (lineUntrimmed in lines) {
            val line = lineUntrimmed.trim()

            if (liteProfileReached && line == "</includes>") {
                break
            }
            else if (line == "<id>lite</id>") {
                liteProfileReached = true
                continue
            }

            if (liteProfileReached && line.startsWith(INCLUDE_START) && line.endsWith(INCLUDE_END)) {
                keepClasses.add(line.removeSurrounding(INCLUDE_START, INCLUDE_END))
            }
        }

        assert(liteProfileReached && keepClasses.isNotEmpty()) { "Wrong pom.xml or the format has changed, check its contents at $POM_PATH" }

        val outputFile = File(outputJarPath).apply { delete() }
        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { output ->
            for ((name, value) in allFiles) {
                val className = name.substringAfter("org/jetbrains/kotlin/protobuf/").substringBeforeLast(".class")
                if (keepClasses.any { className == it || className.startsWith(it + "$") }) {
                    val (entry, bytes) = value
                    output.putNextEntry(entry)
                    output.write(bytes)
                    output.closeEntry()
                }
            }
        }
    }
}

val mainArtifact = artifacts.add(
    "default",
    provider {
        prepare.outputs.files.singleFile
    }
) {
    builtBy(prepare)
    classifier = ""
}

val sourcesArtifact = artifacts.add(
    "default",
    provider {
        relocatedProtobuf.resolvedConfiguration.resolvedArtifacts.single { it.name == "protobuf-relocated" && it.classifier == "sources" }.file
    }
) {
    classifier = "sources"
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(mainArtifact)
            artifact(sourcesArtifact)
        }
    }

    repositories {
        maven {
            url = uri("${rootProject.buildDir}/internal/repo")
        }
    }
}
