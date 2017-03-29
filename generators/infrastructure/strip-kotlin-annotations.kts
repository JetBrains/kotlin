/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jetbrains.org.objectweb.asm.*
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.zip.ZipOutputStream

/**
 * Removes metadata annotations from Kotlin classes
 */

fun main(args: Array<String>) {
    if (args.size != 4) {
        error("Usage: kotlinc -script strip-kotlin-annotations.kts <annotation-internal-name-regex> <class-internal-name-regex> <path-to-in-jar> <path-to-out-jar>")
    }

    val annotationRegex = args[0].toRegex()
    val classRegex = args[1].toRegex()
    val inFile = File(args[2])
    val outFile = File(args[3])

    assert(inFile.exists()) { "Input file not found at $inFile" }

    println("Stripping annotations from all classes in $inFile")
    println("Input file size: ${inFile.length()} bytes")

    fun transform(entryName: String, bytes: ByteArray): ByteArray {
        if (!entryName.endsWith(".class")) return bytes
        if (!classRegex.matches(entryName.removeSuffix(".class"))) return bytes

        var changed = false
        val classWriter = ClassWriter(0)
        val classVisitor = object : ClassVisitor(Opcodes.ASM5, classWriter) {
            override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
                if (annotationRegex.matches(Type.getType(desc).getInternalName())) {
                    changed = true
                    return null
                }
                return super.visitAnnotation(desc, visible)
            }
        }
        ClassReader(bytes).accept(classVisitor, 0)
        if (!changed) return bytes

        return classWriter.toByteArray()
    }

    ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use {
        outJar ->
        JarFile(inFile).use { inJar ->
            for (entry in inJar.entries()) {
                val inBytes = inJar.getInputStream(entry).readBytes()
                val outBytes = transform(entry.name, inBytes)

                if (inBytes.size < outBytes.size) {
                    error("Size increased for ${entry.name}: was ${inBytes.size} bytes, became ${outBytes.size} bytes")
                }

                entry.compressedSize = -1L
                outJar.putNextEntry(entry)
                outJar.write(outBytes)
                outJar.closeEntry()
            }
        }
    }

    println("Output written to $outFile")
    println("Output file size: ${outFile.length()} bytes")
}

main(args)
