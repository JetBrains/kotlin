/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.IrTypeAbbreviationImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.utils.memoryOptimizedMap

class SimpleTypeRemapper(
    private val symbolRemapper: SymbolRemapper
) : TypeRemapper {

    override fun enterScope(irTypeParametersContainer: IrTypeParametersContainer) {
    }

    override fun leaveScope() {
    }

    override fun remapType(type: IrType): IrType =
        if (type !is IrSimpleType)
            type
        else {
            val symbol = symbolRemapper.getReferencedClassifier(type.classifier)
            val arguments = type.arguments.memoryOptimizedMap { remapTypeArgument(it) }
            if (symbol == type.classifier && arguments == type.arguments)
                type
            else {
                IrSimpleTypeImpl(
                    null,
                    symbol,
                    type.nullability,
                    arguments,
                    type.annotations,
                    type.abbreviation?.remapTypeAbbreviation()
                )
            }
        }

    private fun remapTypeArgument(typeArgument: IrTypeArgument): IrTypeArgument =
        when (typeArgument) {
            is IrTypeProjection -> makeTypeProjection(this.remapType(typeArgument.type), typeArgument.variance)
            is IrStarProjection -> typeArgument
        }

    private fun IrTypeAbbreviation.remapTypeAbbreviation() =
        IrTypeAbbreviationImpl(
            symbolRemapper.getReferencedTypeAlias(typeAlias),
            hasQuestionMark,
            arguments.memoryOptimizedMap { remapTypeArgument(it) },
            annotations
        )
}
