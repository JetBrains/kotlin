/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.types

import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.name.ClassId

private object KtTypeRenderer {
    fun render(type: KtType, options: KtTypeRendererOptions = KtTypeRendererOptions.DEFAULT): String = type.withValidityAssertion {
        buildString { render(type, options) }
    }

    private fun StringBuilder.render(type: KtType, options: KtTypeRendererOptions) {
        when (type) {
            is KtDenotableType -> when (type) {
                is KtClassType -> {
                    render(type.classId, options)
                    renderTypeArgumentsIfNotEmpty(type.typeArguments, options)
                    renderNullability(type.nullability)
                }
                is KtTypeParameterType -> {
                    append(type.name.asString())
                    renderNullability(type.nullability)
                }
            }
            is KtNonDenotableType -> when (type) {
                is KtFlexibleType -> inParens {
                    render(type.lowerBound, options)
                    append("..")
                    render(type.upperBound, options)
                }
                is KtIntersectionType -> inParens {
                    type.conjuncts.forEachIndexed { index, conjunct ->
                        render(conjunct, options)
                        if (index != type.conjuncts.lastIndex) {
                            append("&")
                        }
                    }
                }
            }
            is KtErrorType -> {
                append(type.error)
            }
            else -> error("Unsupported type ${type::class}")
        }
    }

    private fun StringBuilder.renderTypeArgumentsIfNotEmpty(typeArguments: List<KtTypeArgument>, options: KtTypeRendererOptions) {
        if (typeArguments.isNotEmpty()) {
            append("<")
            typeArguments.forEachIndexed { index, typeArgument ->
                render(typeArgument, options)
                if (index != typeArguments.lastIndex) {
                    append(", ")
                }
            }
            append(">")
        }
    }

    private fun StringBuilder.render(typeArgument: KtTypeArgument, options: KtTypeRendererOptions) {
        when (typeArgument) {
            KtStarProjectionTypeArgument -> {
                append("*")
            }
            is KtTypeArgumentWithVariance -> {
                val varianceWithSpace = when (typeArgument.variance) {
                    KtTypeArgumentVariance.COVARIANT -> "out "
                    KtTypeArgumentVariance.CONTRAVARIANT -> "in "
                    KtTypeArgumentVariance.INVARIANT -> ""
                }
                append(varianceWithSpace)
                render(typeArgument.type, options)
            }
        }
    }

    private fun StringBuilder.renderNullability(nullability: KtTypeNullability) {
        if (nullability == KtTypeNullability.NULLABLE) {
            append("?")
        }
    }

    private fun StringBuilder.render(classId: ClassId, options: KtTypeRendererOptions) {
        if (options.renderFqNames) {
            append(classId.asString().replace('/', '.'))
        } else {
            append(classId.shortClassName.asString())
        }
    }

    private inline fun StringBuilder.inParens(render: StringBuilder.() -> Unit) {
        append("(")
        render()
        append(")")

    }
}

data class KtTypeRendererOptions(
    val renderFqNames: Boolean,
) {
    companion object {
        val DEFAULT = KtTypeRendererOptions(
            renderFqNames = true
        )
    }
}

fun KtType.render(options: KtTypeRendererOptions = KtTypeRendererOptions.DEFAULT): String =
    KtTypeRenderer.render(this, options)

