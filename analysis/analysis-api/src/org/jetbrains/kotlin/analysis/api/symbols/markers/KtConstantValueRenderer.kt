/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols.markers

import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.types.ConstantValueKind

public object KtConstantValueRenderer {
    public fun render(value: KtAnnotationValue): String = buildString {
        renderConstantValue(value)
    }

    private fun StringBuilder.renderConstantValue(value: KtAnnotationValue) {
        when (value) {
            is KtAnnotationAnnotationValue -> {
                renderAnnotationConstantValue(value)
            }
            is KtArrayAnnotationValue -> {
                renderArrayConstantValue(value)
            }
            is KtEnumEntryAnnotationValue -> {
                renderEnumEntryConstantValue(value)
            }
            is KtErrorValue -> {
                append("ERROR")
            }
            is KtLiteralAnnotationValue<*> -> {
                renderLiteralConstantValue(value)
            }
            KtUnsupportedAnnotationValue -> {
                append("KtUnsupportedConstantValue")
            }
        }
    }

    private fun StringBuilder.renderLiteralConstantValue(value: KtLiteralAnnotationValue<*>) {
        when (value.constantValueKind) {
            ConstantValueKind.String -> {
                append('"')
                append(value.value)
                append('"')
            }
            ConstantValueKind.Char -> {
                append("'")
                append(value.value)
                append("'")
            }
            else -> {
                append(value.value)
            }
        }
    }

    private fun StringBuilder.renderEnumEntryConstantValue(value: KtEnumEntryAnnotationValue) {
        append(value.callableId?.asSingleFqName()?.asString())
    }

    private fun StringBuilder.renderAnnotationConstantValue(value: KtAnnotationAnnotationValue) {
        append(value.classId)
        if (value.arguments.isNotEmpty()) {
            append("(")
            renderNamedConstantValueList(value.arguments)
            append(")")
        }
    }

    private fun StringBuilder.renderArrayConstantValue(value: KtArrayAnnotationValue) {
        append("[")
        renderConstantValueList(value.values)
        append("]")
    }

    private fun StringBuilder.renderConstantValueList(list: Collection<KtAnnotationValue>) {
        renderWithSeparator(list, ", ") { constantValue ->
            renderConstantValue(constantValue)
        }
    }

    private fun StringBuilder.renderNamedConstantValueList(list: Collection<KtNamedConstantValue>) {
        renderWithSeparator(list, ", ") { namedValue ->
            append(namedValue.name)
            append(" = ")
            renderConstantValue(namedValue.expression)
            append(", ")
        }
    }

    private inline fun <E> StringBuilder.renderWithSeparator(
        collection: Collection<E>,
        separator: String,
        render: StringBuilder.(E) -> Unit
    ) {
        collection.forEachIndexed { index, element ->
            render(element)
            if (index != collection.size - 1) {
                append(separator)
            }
        }
    }
}