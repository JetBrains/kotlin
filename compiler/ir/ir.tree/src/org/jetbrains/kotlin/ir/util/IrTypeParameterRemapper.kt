/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.utils.memoryOptimizedMap

/**
 * After moving an [org.jetbrains.kotlin.ir.IrElement], some type parameter references within it may become out of scope.
 * This remapper restores validity by redirecting those references to new type parameters.
 */
open class IrTypeParameterRemapper(
    private val typeParameterMap: Map<IrTypeParameter, IrTypeParameter>
) : AbstractTypeRemapper() {

    override fun remapTypeOrNull(type: IrType): IrType? {
        if (type !is IrSimpleType) return null
        val classifier = type.classifier.remap()
        val arguments = remapTypeArguments(type.arguments)
        if (classifier === type.classifier && arguments == null && type.annotations.isEmpty() && type.abbreviation == null) {
            return null
        }
        return IrSimpleTypeImpl(
            classifier,
            type.nullability,
            arguments ?: type.arguments,
            type.annotations,
            type.abbreviation?.remap()
        ).apply {
            annotations.forEach { it.remapTypes(this@IrTypeParameterRemapper) }
        }
    }

    private fun IrClassifierSymbol.remap() =
        (owner as? IrTypeParameter)?.let { typeParameterMap[it]?.symbol }
            ?: this

    private fun IrTypeArgument.remap() =
        when (this) {
            is IrTypeProjection -> makeTypeProjection(remapType(type), variance)
            is IrStarProjection -> this
        }

    private fun IrTypeAbbreviation.remap() =
        IrTypeAbbreviationImpl(
            typeAlias,
            hasQuestionMark,
            arguments.memoryOptimizedMap { it.remap() },
            annotations
        ).apply {
            annotations.forEach { it.remapTypes(this@IrTypeParameterRemapper) }
        }
}
