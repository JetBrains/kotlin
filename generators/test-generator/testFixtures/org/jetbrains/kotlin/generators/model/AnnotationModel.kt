/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.model

import org.jetbrains.kotlin.generators.util.isDefaultImportedClass
import org.jetbrains.kotlin.utils.Printer

class AnnotationModel(
    val annotation: Class<out Annotation>,
    val arguments: List<AnnotationArgumentModel>
) {
    fun generate(p: Printer) {
        val needExplicitNames = arguments.singleOrNull()?.name != AnnotationArgumentModel.DEFAULT_NAME
        val argumentsString = arguments.joinToString(separator = ", ") { argument ->
            val valueString = when (val value = argument.value) {
                is Enum<*> -> "${value.javaClass.simpleName}.${value.name}"
                is Array<*> -> value.toJavaString()
                is Class<*> -> "${value.simpleName}.class"
                else -> "\"$value\""
            }
            if (needExplicitNames) "${argument.name} = $valueString" else valueString
        }
        p.print("@${annotation.simpleName}($argumentsString)")
    }

    private fun Array<*>.toJavaString(): String =
        buildString {
            append("{ ")
            append(this@toJavaString.joinToString(separator = ", ") { "\"$it\"" })
            append(" }")
        }

    @OptIn(ExperimentalStdlibApi::class)
    fun imports(): List<Class<*>> {
        return buildList {
            add(annotation)
            arguments.mapNotNullTo(this) { argument ->
                when (val value = argument.value) {
                    is Enum<*> -> value.javaClass
                    is Class<*> -> value
                    else -> null
                }
            }
        }.filterNot { it.isDefaultImportedClass() }
    }
}

fun annotation(annotation: Class<out Annotation>, singleArgumentValue: Any): AnnotationModel {
    return AnnotationModel(annotation, listOf(AnnotationArgumentModel(value = singleArgumentValue)))
}

fun annotation(annotation: Class<out Annotation>, vararg arguments: Pair<String, Any>): AnnotationModel {
    return AnnotationModel(annotation, arguments.map { AnnotationArgumentModel(it.first, it.second) })
}
