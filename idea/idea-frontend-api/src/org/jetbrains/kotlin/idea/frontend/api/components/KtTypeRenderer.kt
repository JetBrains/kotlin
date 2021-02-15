/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.*
import org.jetbrains.kotlin.idea.frontend.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

abstract class KtTypeRenderer : KtAnalysisSessionComponent() {
    abstract fun render(type: KtType, options: KtTypeRendererOptions): String
}

data class KtTypeRendererOptions(
    val renderFqNames: Boolean,
    val renderFunctionTypes: Boolean
) {
    companion object {
        val DEFAULT = KtTypeRendererOptions(
            renderFqNames = true,
            renderFunctionTypes = true,
        )
        val SHORT_NAMES = KtTypeRendererOptions(
            renderFqNames = false,
            renderFunctionTypes = true,
        )
    }
}

class KtDefaultTypeRenderer(override val analysisSession: KtAnalysisSession, override val token: ValidityToken) : KtTypeRenderer() {
    override fun render(type: KtType, options: KtTypeRendererOptions): String = type.withValidityAssertion {
        buildString { render(type, options) }
    }

    private fun StringBuilder.render(type: KtType, options: KtTypeRendererOptions) {
        when (type) {
            is KtDenotableType -> when (type) {
                is KtClassType -> {
                    renderClassType(type, options)
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

    private fun StringBuilder.renderClassType(
        type: KtClassType,
        options: KtTypeRendererOptions
    ) {
        when {
            type is KtUsualClassType || !options.renderFunctionTypes -> {
                renderClassTypeAsNonFunctional(type, options)
            }
            type is KtFunctionalType -> {
                renderFunctionalType(type, options)
            }
        }
    }

    private fun StringBuilder.renderClassTypeAsNonFunctional(
        type: KtClassType,
        options: KtTypeRendererOptions
    ) {
        render(type.classId, options)
        renderTypeArgumentsIfNotEmpty(type.typeArguments, options)
        renderNullability(type.nullability)
    }

    private fun StringBuilder.renderFunctionalType(
        type: KtFunctionalType,
        options: KtTypeRendererOptions
    ) {
        wrapIf(type.nullability == KtTypeNullability.NULLABLE, left = "(", right = ")?") {
            if (type.isSuspend) append("suspend ")
            renderReceiverTypeOfFunctionalType(type, options)
            renderParametersOfFunctionalType(type, options)
            append(" -> ")
            render(type.typeArguments.last(), options)
        }
    }

    private fun StringBuilder.renderReceiverTypeOfFunctionalType(
        type: KtFunctionalType,
        options: KtTypeRendererOptions
    ) {
        type.receiverType?.let { receiverType ->
            render(receiverType, options)
            append(".")
        }
    }

    private fun StringBuilder.renderParametersOfFunctionalType(
        type: KtFunctionalType,
        options: KtTypeRendererOptions
    ) {
        append("(")
        val parameterTypes = type.parameterTypes
        type.parameterTypes.forEachIndexed { index, parameterType ->
            render(parameterType, options)
            if (index != parameterTypes.lastIndex) {
                append(", ")
            }
        }
        append(")")
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
                    Variance.OUT_VARIANCE -> "out "
                    Variance.IN_VARIANCE -> "in "
                    Variance.INVARIANT -> ""
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

    private inline fun StringBuilder.wrapIf(condition: Boolean, left: String, right: String, render: StringBuilder.() -> Unit) {
        if (condition) append(left)
        render()
        if (condition) append(right)
    }
}