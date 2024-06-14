/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.renderer.render

internal object KaAnnotationValueRenderer {
    fun render(value: KaAnnotationValue): String = buildString {
        renderConstantValue(value)
    }

    private fun StringBuilder.renderConstantValue(value: KaAnnotationValue) {
        when (value) {
            is KaAnnotationValue.NestedAnnotationValue -> {
                renderAnnotationConstantValue(value)
            }
            is KaAnnotationValue.ArrayValue -> {
                renderArrayConstantValue(value)
            }
            is KaAnnotationValue.EnumEntryValue -> {
                renderEnumEntryConstantValue(value)
            }
            is KaAnnotationValue.ConstantValue -> {
                renderConstantAnnotationValue(value)
            }
            is KaAnnotationValue.UnsupportedValue -> {
                append("error(\"non-annotation value\")")
            }
            is KaAnnotationValue.ClassLiteralValue -> {
                renderKClassAnnotationValue(value)
            }
        }
    }

    private fun StringBuilder.renderKClassAnnotationValue(value: KaAnnotationValue.ClassLiteralValue) {
        renderType(value.type)
        append("::class")
    }

    private fun StringBuilder.renderType(type: KaType) {
        if (type.annotations.isNotEmpty()) {
            for (annotation in type.annotations) {
                append('@')
                renderAnnotationApplication(annotation)
                append(' ')
            }
        }

        when (type) {
            is KaUsualClassType -> {
                val classId = type.classId
                if (classId.isLocal) {
                    append(classId.shortClassName.render())
                } else {
                    append(classId.asSingleFqName().render())
                }

                if (type.typeArguments.isNotEmpty()) {
                    append('<')
                    renderWithSeparator(type.typeArguments, ", ") { typeProjection ->
                        when (typeProjection) {
                            is KaStarTypeProjection -> append('*')
                            is KaTypeArgumentWithVariance -> renderType(typeProjection.type)
                        }
                    }
                    append('>')
                }
            }
            is KaClassErrorType -> {
                append("UNRESOLVED_CLASS")
            }
            else -> {
                append(type)
            }
        }
    }

    private fun StringBuilder.renderConstantAnnotationValue(value: KaAnnotationValue.ConstantValue) {
        append(value.value.render())
    }

    private fun StringBuilder.renderEnumEntryConstantValue(value: KaAnnotationValue.EnumEntryValue) {
        append(value.callableId?.asSingleFqName()?.asString())
    }

    private fun StringBuilder.renderAnnotationConstantValue(application: KaAnnotationValue.NestedAnnotationValue) {
        renderAnnotationApplication(application.annotation)
    }

    private fun StringBuilder.renderAnnotationApplication(value: KaAnnotation) {
        append(value.classId)
        if (value.arguments.isNotEmpty()) {
            append("(")
            renderNamedConstantValueList(value.arguments)
            append(")")
        }
    }

    private fun StringBuilder.renderArrayConstantValue(value: KaAnnotationValue.ArrayValue) {
        append("[")
        renderConstantValueList(value.values)
        append("]")
    }

    private fun StringBuilder.renderConstantValueList(list: Collection<KaAnnotationValue>) {
        renderWithSeparator(list, ", ") { constantValue ->
            renderConstantValue(constantValue)
        }
    }

    private fun StringBuilder.renderNamedConstantValueList(list: Collection<KaNamedAnnotationValue>) {
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