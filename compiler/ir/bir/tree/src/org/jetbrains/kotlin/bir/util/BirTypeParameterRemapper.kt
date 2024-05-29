/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.symbols.BirClassifierSymbol
import org.jetbrains.kotlin.bir.symbols.BirTypeParameterSymbol
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class BirTypeParameterRemapper(
    private val typeParameterMap: Map<BirTypeParameter, BirTypeParameter>
) {
    fun remapType(type: BirType): BirType =
        if (type !is BirSimpleType)
            type
        else
            BirSimpleTypeImpl(
                null,
                type.classifier.remap(),
                type.nullability,
                type.arguments.memoryOptimizedMap { it.remap() },
                type.annotations,
                type.abbreviation?.remap()
            ).apply {
                annotations.forEach { it.remapTypes(this@BirTypeParameterRemapper) }
            }

    private fun BirClassifierSymbol.remap(): BirClassifierSymbol =
        (this as? BirTypeParameterSymbol)?.let { typeParameterMap[it.owner]?.symbol }
            ?: this

    private fun BirTypeArgument.remap() =
        when (this) {
            is BirTypeProjection -> makeTypeProjection(remapType(type), variance)
            is BirStarProjection -> this
        }

    private fun BirTypeAbbreviation.remap() =
        BirTypeAbbreviation(
            typeAlias,
            hasQuestionMark,
            arguments.memoryOptimizedMap { it.remap() },
            annotations
        ).apply {
            annotations.forEach { it.remapTypes(this@BirTypeParameterRemapper) }
        }
}