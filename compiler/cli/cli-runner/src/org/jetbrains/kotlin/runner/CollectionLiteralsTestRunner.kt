/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.runner

import java.io.File
import java.net.URL

private val KOTLIN_HOME = File("D:\\projects\\kotlin\\dist\\kotlinc")

private val SIMPLE_FIR_TEST = "D:\\projects\\kotlin\\compiler\\cli\\cli-runner\\testData\\firTest.kt"

fun main() {
    val cp = listOf(
        "lib/kotlin-stdlib.jar",
        "lib/kotlin-reflect.jar"
    ).map(::resolveKt)

    val compilerCp = listOf(
        "lib/kotlin-compiler.jar"
    ).map(::resolveKt)

    val runner = ReplRunner()
    runner.run(cp, listOf("-Xuse-fir", SIMPLE_FIR_TEST), emptyList(), compilerCp)
}

private fun File.toURL2(): URL {
    return absoluteFile.toURI().toURL()
}

private fun resolveKt(s: String): URL {
    return KOTLIN_HOME.resolve(s).toURL2()
}