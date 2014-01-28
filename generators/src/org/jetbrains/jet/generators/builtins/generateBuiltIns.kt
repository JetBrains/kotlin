/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.builtins

import org.jetbrains.jet.generators.builtins.functions.*
import org.jetbrains.jet.generators.builtins.iterators.*
import org.jetbrains.jet.generators.builtins.progressionIterators.*
import org.jetbrains.jet.generators.builtins.progressions.*
import org.jetbrains.jet.generators.builtins.ranges.*
import java.io.PrintWriter
import java.io.File
import kotlin.properties.Delegates

fun existingDirectory(path: String): File {
    val result = File(path)
    if (!result.exists()) {
        throw IllegalStateException("Output dir does not exist: ${result.getAbsolutePath()}")
    }
    return result
}

val BUILT_INS_DIR: File by Delegates.lazy { existingDirectory("core/builtins/src/jet/") }
val RUNTIME_JVM_DIR: File by Delegates.lazy { existingDirectory("core/runtime.jvm/src/jet/") }

fun generateFile(dir: File, name: String, generator: (PrintWriter) -> BuiltInsSourceGenerator) {
    val file = File(dir, name)
    println("generating $file")
    PrintWriter(file) use {
        fileHeader(it)
        generator(it).generate()
    }
}

fun fileHeader(out: PrintWriter) {
    out.println(File("injector-generator/copyright.txt").readText())
    // Don't include generator class name in the message: these are built-in sources,
    // and we don't want to scare users with any internal information about our project
    out.println("// Auto-generated file. DO NOT EDIT!")
    out.println()
    out.println("package jet")
    out.println()
}


fun main(args: Array<String>) {
    fun generateBuiltInFile(name: String, generator: (PrintWriter) -> BuiltInsSourceGenerator) {
        generateFile(BUILT_INS_DIR, name, generator)
    }

    fun generateRuntimeJvmFile(name: String, generator: (PrintWriter) -> BuiltInsSourceGenerator) {
        generateFile(RUNTIME_JVM_DIR, name, generator)
    }

    for (kind in FunctionKind.values()) {
        generateBuiltInFile(kind.getFileName()) { GenerateFunctions(it, kind) }
        generateRuntimeJvmFile(kind.getImplFileName()) { GenerateFunctionsImpl(it, kind) }
    }

    generateBuiltInFile("Iterators.kt") { GenerateIterators(it) }
    generateBuiltInFile("ProgressionIterators.kt") { GenerateProgressionIterators(it) }
    generateBuiltInFile("Progressions.kt") { GenerateProgressions(it) }
    generateBuiltInFile("Ranges.kt") { GenerateRanges(it) }
}
