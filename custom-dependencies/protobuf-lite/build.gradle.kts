
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

buildscript {
    repositories {
        jcenter()
    }

    dependencies {
        classpath("com.github.jengelman.gradle.plugins:shadow:1.2.3")
    }
}

apply { plugin("com.github.johnrengelman.shadow") }

repositories {
    mavenCentral()
}

val protobufCfg = configurations.create("protobuf-java")

val protobufVersion = "2.6.1"
val protobufJarPrefix = "protobuf-$protobufVersion"
val renamedOutputJarPathWithoutExt = "$buildDir/jars/$protobufJarPrefix-relocated"
val renamedOutputJarPath = "$renamedOutputJarPathWithoutExt.jar"
val outputJarPath = "$buildDir/libs/$protobufJarPrefix-lite.jar"

artifacts.add("protobuf-java", File(outputJarPath))

dependencies {
    "protobuf-java"("com.google.protobuf:protobuf-java:$protobufVersion")
}

val relocateTask = task<ShadowJar>("relocate-protobuf") {
    classifier = renamedOutputJarPathWithoutExt // TODO: something fishy about the usage here, according to docs only suffix is enough here, but it doesn't work
    this.configurations = listOf(protobufCfg)
    from(protobufCfg.files.find { it.name.startsWith("protobuf-java") }?.canonicalPath)
//    into(jarsDir)
//    doFirst {
//        File(jarsDir).mkdirs()
//    }
    relocate("com.google.protobuf", "org.jetbrains.kotlin.protobuf" ) {
        exclude("META-INF/maven/com.google.protobuf/protobuf-java/pom.properties")
    }
}

val prepareTask = task("prepare") {
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

defaultTasks("prepare")

