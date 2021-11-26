/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.generators.util.isDefaultImportedClass
import org.jetbrains.kotlin.utils.Printer

class AnnotationModel(
    val annotation: Class<out Annotation>,
    val arguments: List<Any>
) {
    fun generate(p: Printer) {
        val argumentsString = arguments.joinToString(separator = ",") {
            when (it) {
                is Enum<*> -> "${it.javaClass.simpleName}.${it.name}"
                is Class<*> -> "${it.simpleName}.class"
                else -> "\"$it\""
            }
        }
        p.print("@${annotation.simpleName}($argumentsString)")
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun imports(): List<Class<*>> {
        return buildList {
            add(annotation)
            arguments.mapNotNullTo(this) {
                when (it) {
                    is Enum<*> -> it.javaClass
                    is Class<*> -> it
                    else -> null
                }
            }
        }.filterNot { it.isDefaultImportedClass() }
    }
}

fun annotation(annotation: Class<out Annotation>, vararg arguments: Any): AnnotationModel {
    return AnnotationModel(annotation, arguments.toList())
}
