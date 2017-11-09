package org.jetbrains.kotlin.infrastructure

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

/**
 * Strip away all unneeded classes from protobuf-java-*.jar to make a LITE version of the protobuf runtime.
 * To do this, we load pom.xml from protobuf jar and heuristically extract information about which classes
 * are kept in the lite runtime.
 * (This could be done with actually parsing the XML, but this solution works just fine at the moment.)
 * Then we take the full protobuf.jar and copy its contents to another jar, except those classes
 * which are not needed in the lite runtime.
 */

fun main(args: Array<String>) {
    val INCLUDE_START = "<include>**/"
    val INCLUDE_END = ".java</include>"
    val POM_PATH = "META-INF/maven/com.google.protobuf/protobuf-java/pom.xml"

    if (args.size != 2) {
        error("Usage: kotlinc -script build-protobuf-lite.kts <path-to-protobuf-jar> <output-path>")
    }

    val jarFile = File(args[0])
    val outputPath = args[1]

    assert(jarFile.exists()) { "protobuf jar not found at $jarFile" }

    fun loadAllFromJar(file: File): Map<String, Pair<JarEntry, ByteArray>> {
        val result = hashMapOf<String, Pair<JarEntry, ByteArray>>()
        JarFile(file).use { jar ->
            for (jarEntry in jar.entries()) {
                result[jarEntry.name] = Pair(jarEntry, jar.getInputStream(jarEntry).readBytes())
            }
        }
        return result
    }

    val allFiles = loadAllFromJar(jarFile)

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

    val outputFile = File(outputPath).apply { delete() }
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

main(args)
