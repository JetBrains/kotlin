/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.annotations

import org.jetbrains.kotlin.analysis.api.KtStarTypeProjection
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.renderer.render

internal object KtAnnotationValueRenderer {
    fun render(value: KtAnnotationValue): String = buildString {
        renderConstantValue(value)
    }

    private fun StringBuilder.renderConstantValue(value: KtAnnotationValue) {
        when (value) {
            is KtAnnotationApplicationValue -> {
                renderAnnotationConstantValue(value)
            }
            is KtArrayAnnotationValue -> {
                renderArrayConstantValue(value)
            }
            is KtEnumEntryAnnotationValue -> {
                renderEnumEntryConstantValue(value)
            }
            is KtConstantAnnotationValue -> {
                renderConstantAnnotationValue(value)
            }
            is KtUnsupportedAnnotationValue -> {
                append("error(\"non-annotation value\")")
            }
            is KtKClassAnnotationValue -> {
                renderKClassAnnotationValue(value)
            }
        }
    }

    private fun StringBuilder.renderKClassAnnotationValue(value: KtKClassAnnotationValue) {
        renderType(value.type)
        append("::class")
    }

    private fun StringBuilder.renderType(type: KtType) {
        if (type.annotations.isNotEmpty()) {
            for (annotation in type.annotations) {
                append('@')
                renderAnnotationApplication(annotation)
                append(' ')
            }
        }

        when (type) {
            is KtUsualClassType -> {
                val classId = type.classId
                if (classId.isLocal) {
                    append(classId.shortClassName.render())
                } else {
                    append(classId.asSingleFqName().render())
                }

                if (type.ownTypeArguments.isNotEmpty()) {
                    append('<')
                    renderWithSeparator(type.ownTypeArguments, ", ") { typeProjection ->
                        when (typeProjection) {
                            is KtStarTypeProjection -> append('*')
                            is KtTypeArgumentWithVariance -> renderType(typeProjection.type)
                        }
                    }
                    append('>')
                }
            }
            is KtClassErrorType -> {
                append("UNRESOLVED_CLASS")
            }
            else -> {
                append(type.asStringForDebugging())
            }
        }
    }

    private fun StringBuilder.renderConstantAnnotationValue(value: KtConstantAnnotationValue) {
        append(value.constantValue.renderAsKotlinConstant())
    }

    private fun StringBuilder.renderEnumEntryConstantValue(value: KtEnumEntryAnnotationValue) {
        append(value.callableId?.asSingleFqName()?.asString())
    }

    private fun StringBuilder.renderAnnotationConstantValue(application: KtAnnotationApplicationValue) {
        renderAnnotationApplication(application.annotationValue)
    }

    private fun StringBuilder.renderAnnotationApplication(value: KtAnnotationApplicationWithArgumentsInfo) {
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

    private fun StringBuilder.renderNamedConstantValueList(list: Collection<KtNamedAnnotationValue>) {
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