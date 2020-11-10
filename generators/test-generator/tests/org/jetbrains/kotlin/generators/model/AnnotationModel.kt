/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.test.MuteExtraSuffix
import org.jetbrains.kotlin.utils.Printer

class AnnotationModel(
    val annotation: Class<out Annotation>,
    val arguments: List<Any>
) {
    fun generate(p: Printer) {
        val argumentsString = arguments.joinToString(separator = ",") { "\"$it\""}
        p.print("@${annotation.simpleName}($argumentsString)")
    }
}

fun muteExtraSuffix(suffix: String) = AnnotationModel(MuteExtraSuffix::class.java, arguments = listOf(suffix))

