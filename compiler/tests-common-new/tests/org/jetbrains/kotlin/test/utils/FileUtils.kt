/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import java.io.File

private const val FIR_KT = ".fir.kt"
private const val KT = ".kt"

val File.isFirTestData: Boolean
    get() = name.endsWith(FIR_KT)

val File.originalTestDataFile: File
    get() = if (isFirTestData) {
        parentFile.resolve("${name.removeSuffix(FIR_KT)}$KT")
    } else {
        this
    }

val File.firTestDataFile: File
    get() = if (isFirTestData) {
        this
    } else {
        parentFile.resolve("${name.removeSuffix(KT)}$FIR_KT")
    }

fun File.withExtension(extension: String): File {
    return withSuffixAndExtension(suffix = "", extension)
}

fun File.withSuffixAndExtension(suffix: String, extension: String): File {
    @Suppress("NAME_SHADOWING")
    val extension = extension.removePrefix(".")
    return parentFile.resolve("$nameWithoutExtension$suffix.$extension")
}

/*
 * Please use this method only in places where `TestModule` is not accessible
 * In other cases use testModule.directives
 */
fun File.isDirectiveDefined(directive: String): Boolean = this.useLines { line ->
    line.any { it == directive }
}
