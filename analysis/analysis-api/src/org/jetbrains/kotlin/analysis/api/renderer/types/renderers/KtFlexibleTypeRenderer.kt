/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.name.StandardClassIds
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


public interface KtFlexibleTypeRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderType(type: KtFlexibleType, printer: PrettyPrinter)

    public object AS_RANGE : KtFlexibleTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtFlexibleType, printer: PrettyPrinter): Unit = printer {
            append('(')
            renderType(type.lowerBound, printer)
            append("..")
            renderType(type.upperBound, printer)
            append(')')
        }
    }

    public object AS_SHORT : KtFlexibleTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtFlexibleType, printer: PrettyPrinter): Unit = printer {
            val lower = type.lowerBound
            val upper = type.upperBound

            when {
                isNullabilityFlexibleType(lower, upper) -> {
                    renderType(lower, printer)
                    append("!")
                }

                isMutabilityFlexibleType(lower, upper) -> {
                    " ".separated(
                        { annotationsRenderer.renderAnnotations(type, printer) },
                        { append(lower.classId.asFqNameString().replace("Mutable", "(Mutable)")) },
                    )
                    printCollectionIfNotEmpty(lower.ownTypeArguments, prefix = "<", postfix = ">") { typeArgument ->
                        typeProjectionRenderer.renderTypeProjection(typeArgument, this)
                    }
                    if (lower.nullability != type.upperBound.nullability) {
                        append('!')
                    }
                }

                else -> {
                    AS_RANGE.renderType(type, printer)
                }
            }
        }

        private fun isNullabilityFlexibleType(lower: KtType, upper: KtType): Boolean {
            val isTheSameType = lower is KtNonErrorClassType && upper is KtNonErrorClassType && lower.classId == upper.classId ||
                    lower is KtTypeParameterType && upper is KtTypeParameterType && lower.symbol == upper.symbol
            if (isTheSameType &&
                lower.nullability == KtTypeNullability.NON_NULLABLE
                && upper.nullability == KtTypeNullability.NULLABLE
            ) {
                if (lower !is KtNonErrorClassType && upper !is KtNonErrorClassType) {
                    return true
                }
                if (lower is KtNonErrorClassType && upper is KtNonErrorClassType) {
                    val lowerOwnTypeArguments = lower.ownTypeArguments
                    val upperOwnTypeArguments = upper.ownTypeArguments
                    if (lowerOwnTypeArguments.size == upperOwnTypeArguments.size) {
                        for ((index, ktTypeProjection) in lowerOwnTypeArguments.withIndex()) {
                            if (upperOwnTypeArguments[index].type != ktTypeProjection.type) {
                                return false
                            }
                        }
                        return true
                    }
                }
            }
            return false
        }

        @OptIn(ExperimentalContracts::class)
        private fun isMutabilityFlexibleType(lower: KtType, upper: KtType): Boolean {
            contract {
                returns(true) implies (lower is KtNonErrorClassType)
                returns(true) implies (upper is KtNonErrorClassType)
            }
            if (lower !is KtNonErrorClassType || upper !is KtNonErrorClassType) return false

            if (StandardClassIds.Collections.mutableCollectionToBaseCollection[lower.classId] != upper.classId) return false
            return true
        }

    }

}

