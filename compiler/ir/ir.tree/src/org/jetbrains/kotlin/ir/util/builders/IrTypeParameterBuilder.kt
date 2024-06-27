/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util.builders

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class IrTypeParameterBuilder : IrDeclarationBuilder<IrTypeParameter> {
    var variance: Variance
    var isReified: Boolean
    val superTypes: MutableList<IrType>
    val symbol: IrTypeParameterSymbol = IrTypeParameterSymbolImpl()

    @PublishedApi internal constructor(name: Name) : super(name) {
        variance = Variance.INVARIANT
        isReified = false
        superTypes = mutableListOf()
    }
    @PublishedApi internal constructor(from: IrTypeParameter) : super(from.name, from) {
        variance = from.variance
        isReified = from.isReified
        superTypes = from.superTypes.toMutableList()
    }

    @PublishedApi
    internal fun build(factory: IrFactory, index: Int, irFunction: IrFunction) : IrTypeParameter = factory.createTypeParameter(
        startOffset = if (startOffset == UNDEFINED_OFFSET) irFunction.startOffset else startOffset,
        endOffset = if (endOffset == UNDEFINED_OFFSET) irFunction.endOffset else endOffset,
        origin = origin,
        name = name,
        symbol = symbol,
        variance = variance,
        index = index,
        isReified = isReified
    ).also {
        it.parent = irFunction
        it.superTypes = superTypes
        it.annotations = annotations.toList()
    }
}