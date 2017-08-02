
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream
import org.gradle.language.assembler.tasks.Assemble

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

apply { plugin("com.github.johnrengelman.shadow") }

val mainCfg = configurations.create("default")
val relocatedCfg = configurations.create("relocated")

val protobufVersion = rootProject.extra["versions.protobuf-java"]
val protobufJarPrefix = "protobuf-$protobufVersion"
val renamedOutputJarPath = "$buildDir/jars/$protobufJarPrefix-relocated.jar"
val outputJarPath = "$buildDir/libs/$protobufJarPrefix-lite.jar"

artifacts.add(mainCfg.name, File(outputJarPath))
artifacts.add(relocatedCfg.name, File(renamedOutputJarPath))

dependencies {
    mainCfg("com.google.protobuf:protobuf-java:$protobufVersion")
}

val relocateTask = task<ShadowJar>("prepare-relocated-protobuf") {
    archiveName = renamedOutputJarPath
    this.configurations = listOf(relocatedCfg)
    from(mainCfg.files.find { it.name.startsWith("protobuf-java") }?.canonicalPath)
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf" ) {
        // TODO: remove "it." after #KT-12848 get addressed
        exclude("META-INF/maven/com.google.protobuf/protobuf-java/pom.properties")
    }
}

val mainTask = task("prepare") {
    dependsOn(relocateTask)
    val inputJar = renamedOutputJarPath
    inputs.files(inputJar)
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
            val jar = JarFile(file)
            try {
                for (jarEntry in jar.entries()) {
                    result[jarEntry.name] = Pair(jarEntry, jar.getInputStream(jarEntry).readBytes())
                }
            }
            finally {
                // Yes, JarFile does not extend Closeable on JDK 6 so we can't use "use" here
                jar.close()
            }
            return result
        }

        val allFiles = loadAllFromJar(File(inputJar))

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

defaultTasks(mainTask.name)

tasks.withType<Assemble>() {
    dependsOn(mainCfg)
}
