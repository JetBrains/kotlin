/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.builtins.generateBuiltIns

import org.jetbrains.kotlin.generators.builtins.arrayIterators.GenerateArrayIterators
import org.jetbrains.kotlin.generators.builtins.arrays.GenerateArrays
import org.jetbrains.kotlin.generators.builtins.functions.GenerateFunctions
import org.jetbrains.kotlin.generators.builtins.iterators.GenerateIterators
import org.jetbrains.kotlin.generators.builtins.numbers.GenerateFloorDivMod
import org.jetbrains.kotlin.generators.builtins.numbers.GeneratePrimitives
import org.jetbrains.kotlin.generators.builtins.progressionIterators.GenerateProgressionIterators
import org.jetbrains.kotlin.generators.builtins.progressions.GenerateProgressions
import org.jetbrains.kotlin.generators.builtins.ranges.GenerateRanges
import org.jetbrains.kotlin.generators.builtins.unsigned.generateUnsignedTypes
import java.io.File
import java.io.PrintWriter

fun assertExists(file: File) {
    if (!file.exists()) error("Output dir does not exist: ${file.absolutePath}")
}

val BUILT_INS_NATIVE_DIR = File("core/builtins/native/")
val BUILT_INS_SRC_DIR = File("core/builtins/src/")
val RUNTIME_JVM_DIR = File("libraries/stdlib/jvm/runtime/")
val UNSIGNED_TYPES_DIR = File("libraries/stdlib/unsigned/src")
val STDLIB_DIR = File("libraries/stdlib/src")

abstract class BuiltInsSourceGenerator(val out: PrintWriter) {
    protected abstract fun generateBody(): Unit

    protected open fun getPackage(): String = "kotlin"

    protected open fun getMultifileClassName(): String? = null

    fun generate() {
        out.println(File("license/COPYRIGHT_HEADER.txt").readText())
        out.println()
        // Don't include generator class name in the message: these are built-in sources,
        // and we don't want to scare users with any internal information about our project
        out.println("// Auto-generated file. DO NOT EDIT!")
        out.println()
        getMultifileClassName()?.let { name ->
            out.println("@file:kotlin.jvm.JvmName(\"$name\")")
            out.println("@file:kotlin.jvm.JvmMultifileClass")
        }
        out.print("package ${getPackage()}")
        out.println()
        out.println()

        generateBody()
    }
}

fun generateBuiltIns(generate: (File, (PrintWriter) -> BuiltInsSourceGenerator) -> Unit) {
    assertExists(BUILT_INS_NATIVE_DIR)
    assertExists(BUILT_INS_SRC_DIR)
    assertExists(RUNTIME_JVM_DIR)
    assertExists(UNSIGNED_TYPES_DIR)

    generate(File(RUNTIME_JVM_DIR, "kotlin/jvm/functions/Functions.kt")) { GenerateFunctions(it) }
    generate(File(BUILT_INS_NATIVE_DIR, "kotlin/Arrays.kt")) { GenerateArrays(it) }
    generate(File(BUILT_INS_NATIVE_DIR, "kotlin/Primitives.kt")) { GeneratePrimitives(it) }
    generate(File(STDLIB_DIR, "kotlin/collections/PrimitiveIterators.kt")) { GenerateIterators(it) }
    generate(File(RUNTIME_JVM_DIR, "kotlin/jvm/internal/ArrayIterators.kt")) { GenerateArrayIterators(it) }
    generate(File(STDLIB_DIR, "kotlin/ranges/ProgressionIterators.kt")) { GenerateProgressionIterators(it) }
    generate(File(STDLIB_DIR, "kotlin/ranges/Progressions.kt")) { GenerateProgressions(it) }
    generate(File(STDLIB_DIR, "kotlin/ranges/PrimitiveRanges.kt")) { GenerateRanges(it) }
    generate(File(STDLIB_DIR, "kotlin/util/FloorDivMod.kt")) { GenerateFloorDivMod(it) }

    generateUnsignedTypes(UNSIGNED_TYPES_DIR, generate)
}

fun main() {
    generateBuiltIns { file, generator ->
        println("generating $file")
        file.parentFile?.mkdirs()
        PrintWriter(file).use {
            generator(it).generate()
        }
    }
}
