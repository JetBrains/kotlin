/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.name.StandardClassIds
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@KaExperimentalApi
public interface KaFlexibleTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaFlexibleType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    @KaExperimentalApi
    public object AS_RANGE : KaFlexibleTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaFlexibleType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                append('(')
                typeRenderer.renderType(analysisSession, type.lowerBound, printer)
                append("..")
                typeRenderer.renderType(analysisSession, type.upperBound, printer)
                append(')')
            }
        }
    }

    @KaExperimentalApi
    public object AS_SHORT : KaFlexibleTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaFlexibleType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val lower = type.lowerBound
                val upper = type.upperBound

                when {
                    analysisSession.isNullabilityFlexibleType(lower, upper) -> {
                        typeRenderer.renderType(analysisSession, lower, printer)
                        append("!")
                    }

                    isMutabilityFlexibleType(lower, upper) -> {
                        " ".separated(
                            { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, printer) },
                            { append(lower.classId.asFqNameString().replace("Mutable", "(Mutable)")) },
                        )
                        printCollectionIfNotEmpty(lower.typeArguments, prefix = "<", postfix = ">") { typeArgument ->
                            typeRenderer.typeProjectionRenderer.renderTypeProjection(analysisSession, typeArgument, typeRenderer, this)
                        }
                        with(analysisSession) {
                            if (type.hasFlexibleNullability) {
                                append('!')
                            }
                        }
                    }

                    else -> {
                        AS_RANGE.renderType(analysisSession, type, typeRenderer, printer)
                    }
                }
            }
        }

        private fun KaSession.isNullabilityFlexibleType(lower: KaType, upper: KaType): Boolean {
            val isTheSameType = lower is KaClassType && upper is KaClassType && lower.classId == upper.classId ||
                    lower is KaTypeParameterType && upper is KaTypeParameterType && lower.symbol == upper.symbol
            if (isTheSameType &&
                !lower.isMarkedNullable
                && upper.isMarkedNullable
            ) {
                if (lower !is KaClassType && upper !is KaClassType) {
                    return true
                }
                if (lower is KaClassType && upper is KaClassType) {
                    val lowerOwnTypeArguments = lower.typeArguments
                    val upperOwnTypeArguments = upper.typeArguments
                    if (lowerOwnTypeArguments.size == upperOwnTypeArguments.size) {
                        for ((index, kaTypeProjection) in lowerOwnTypeArguments.withIndex()) {
                            if (upperOwnTypeArguments[index].type != kaTypeProjection.type) {
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
        private fun isMutabilityFlexibleType(lower: KaType, upper: KaType): Boolean {
            contract {
                returns(true) implies (lower is KaClassType)
                returns(true) implies (upper is KaClassType)
            }
            if (lower !is KaClassType || upper !is KaClassType) return false

            if (StandardClassIds.Collections.mutableCollectionToBaseCollection[lower.classId] != upper.classId) return false
            return true
        }

    }
}
