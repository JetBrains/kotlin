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

package org.jetbrains.kotlin.infrastructure

import java.io.File
import java.util.jar.JarFile
import org.jetbrains.org.objectweb.asm.*

/**
 * Checks that the two given Kotlin libraries have the same ABI version.
 * If versions are equal, returns with exit code 0, otherwise returns with exit code 3.
 * Some additional information is printed to stderr
 */

fun loadClassVersions(bytes: ByteArray): Pair<List<Int>, List<Int>>? {
    var metadata: IntArray? = null
    var bytecode: IntArray? = null
    ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM5) {
        override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? {
            if (desc != "Lkotlin/Metadata;") return null

            return object : AnnotationVisitor(Opcodes.ASM5) {
                override fun visit(name: String, value: Any) {
                    when (name) {
                        "mv" -> metadata = value as IntArray
                        "bv" -> bytecode = value as IntArray
                    }
                }
            }
        }
    }, 0)
    return if (metadata != null && bytecode != null) Pair(metadata!!.toList(), bytecode!!.toList()) else null
}

fun loadVersions(library: File): String {
    JarFile(library).use { jarFile ->
        for (entry in jarFile.entries()) {
            if (entry.name.endsWith(".class")) {
                val classBytes = jarFile.getInputStream(entry).readBytes()
                loadClassVersions(classBytes)?.let {
                    val (metadata, bytecode) = it
                    return "metadata = ${metadata.joinToString(".")}, bytecode = ${bytecode.joinToString(".")}"
                }
            }
        }
    }
    
    error("No Kotlin classes were found in $library")
}

fun main(args: Array<String>) {
    if (args.size != 2) {
        error("Usage: kotlinc -script check-library-abi-version.kts <jar-1> <jar-2>")
    }
    
    val library1 = File(args[0])
    val library2 = File(args[1])

    val v1 = loadVersions(library1)
    val v2 = loadVersions(library2)

    if (v1 != v2) {
        System.err.println("ABI versions differ:")
        System.err.println("  $library1: $v1")
        System.err.println("  $library2: $v2")
        System.exit(3)
    }
}

main(args)
