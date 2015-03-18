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

package org.jetbrains.kotlin.generators.builtins.generateBuiltIns

import org.jetbrains.kotlin.generators.builtins.arrayIterators.GenerateArrayIterators
import org.jetbrains.kotlin.generators.builtins.arrays.GenerateArrays
import org.jetbrains.kotlin.generators.builtins.functions.FunctionKind
import org.jetbrains.kotlin.generators.builtins.functions.GenerateFunctions
import org.jetbrains.kotlin.generators.builtins.iterators.GenerateIterators
import org.jetbrains.kotlin.generators.builtins.progressionIterators.GenerateProgressionIterators
import org.jetbrains.kotlin.generators.builtins.progressions.GenerateProgressions
import org.jetbrains.kotlin.generators.builtins.ranges.GeneratePrimitives
import org.jetbrains.kotlin.generators.builtins.ranges.GenerateRanges
import java.io.File
import java.io.PrintWriter

fun assertExists(file: File): Unit =
        if (!file.exists()) error("Output dir does not exist: ${file.getAbsolutePath()}")

val BUILT_INS_NATIVE_DIR = File("core/builtins/native/kotlin/")
val BUILT_INS_SRC_DIR = File("core/builtins/src/kotlin/")
val RUNTIME_JVM_DIR = File("core/runtime.jvm/src/kotlin/")

abstract class BuiltInsSourceGenerator(val out: PrintWriter) {
    protected abstract fun generateBody(): Unit

    protected open fun getPackage(): String = "kotlin"

    final fun generate() {
        out.println(File("license/LICENSE.txt").readText())
        // Don't include generator class name in the message: these are built-in sources,
        // and we don't want to scare users with any internal information about our project
        out.println("// Auto-generated file. DO NOT EDIT!")
        out.println()
        out.println("package ${getPackage()}")
        out.println()

        generateBody()
    }
}

fun generateBuiltIns(generate: (File, (PrintWriter) -> BuiltInsSourceGenerator) -> Unit) {
    assertExists(BUILT_INS_NATIVE_DIR)
    assertExists(BUILT_INS_SRC_DIR)
    assertExists(RUNTIME_JVM_DIR)

    for (kind in FunctionKind.values()) {
        generate(File(BUILT_INS_SRC_DIR, kind.getFileName())) { GenerateFunctions(it, kind) }
    }

    generate(File(BUILT_INS_NATIVE_DIR, "Arrays.kt")) { GenerateArrays(it) }
    generate(File(BUILT_INS_NATIVE_DIR, "Primitives.kt")) { GeneratePrimitives(it) }
    generate(File(BUILT_INS_SRC_DIR, "Iterators.kt")) { GenerateIterators(it) }
    generate(File(RUNTIME_JVM_DIR, "jvm/internal/ArrayIterators.kt")) { GenerateArrayIterators(it) }
    generate(File(BUILT_INS_SRC_DIR, "ProgressionIterators.kt")) { GenerateProgressionIterators(it) }
    generate(File(BUILT_INS_SRC_DIR, "Progressions.kt")) { GenerateProgressions(it) }
    generate(File(BUILT_INS_SRC_DIR, "Ranges.kt")) { GenerateRanges(it) }
}

fun main(args: Array<String>) {
    generateBuiltIns { file, generator ->
        println("generating $file")
        file.getParentFile()?.mkdirs()
        PrintWriter(file) use {
            generator(it).generate()
        }
    }
}
