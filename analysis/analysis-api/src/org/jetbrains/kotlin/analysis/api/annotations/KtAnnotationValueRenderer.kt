/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.renderer.render

internal object KaAnnotationValueRenderer {
    fun render(value: KaAnnotationValue): String = buildString {
        renderConstantValue(value)
    }

    private fun StringBuilder.renderConstantValue(value: KaAnnotationValue) {
        when (value) {
            is KaAnnotationApplicationValue -> {
                renderAnnotationConstantValue(value)
            }
            is KaArrayAnnotationValue -> {
                renderArrayConstantValue(value)
            }
            is KaEnumEntryAnnotationValue -> {
                renderEnumEntryConstantValue(value)
            }
            is KaConstantAnnotationValue -> {
                renderConstantAnnotationValue(value)
            }
            is KaUnsupportedAnnotationValue -> {
                append("error(\"non-annotation value\")")
            }
            is KaKClassAnnotationValue -> {
                renderKClassAnnotationValue(value)
            }
        }
    }

    private fun StringBuilder.renderKClassAnnotationValue(value: KaKClassAnnotationValue) {
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

    private fun StringBuilder.renderConstantAnnotationValue(value: KaConstantAnnotationValue) {
        append(value.constantValue.renderAsKotlinConstant())
    }

    private fun StringBuilder.renderEnumEntryConstantValue(value: KaEnumEntryAnnotationValue) {
        append(value.callableId?.asSingleFqName()?.asString())
    }

    private fun StringBuilder.renderAnnotationConstantValue(application: KaAnnotationApplicationValue) {
        renderAnnotationApplication(application.annotationValue)
    }

    private fun StringBuilder.renderAnnotationApplication(value: KaAnnotationApplicationWithArgumentsInfo) {
        append(value.classId)
        if (value.arguments.isNotEmpty()) {
            append("(")
            renderNamedConstantValueList(value.arguments)
            append(")")
        }
    }

    private fun StringBuilder.renderArrayConstantValue(value: KaArrayAnnotationValue) {
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