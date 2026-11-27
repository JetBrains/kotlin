/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:JvmName("IrDeclarationBuilders")
@file:JvmMultifileClass
@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.ir.builders.declarations.v2

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

class IrTypeParameterBuilder internal constructor() : IrDeclarationWithNameBuilder() {
    var variance: Variance = Variance.INVARIANT

    var isReified: Boolean = false

    var superTypes: List<IrType> = []

    internal val symbol: IrTypeParameterSymbol = IrTypeParameterSymbolImpl()
}

internal fun IrFactory.build(builder: IrTypeParameterBuilder, index: Int): IrTypeParameter {
    val node = createTypeParameter(
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        origin = builder.origin,
        name = builder.name,
        variance = builder.variance,
        isReified = builder.isReified,
        symbol = builder.symbol,
        index = index,
    )
    node.annotations = builder.annotations
    node.superTypes = builder.superTypes
    builder.parent?.let { node.parent = it }
    return node
}

abstract class IrTypeParametersContainerBuilder internal constructor() : IrDeclarationWithNameBuilder() {
    internal val typeParameters: MutableList<IrTypeParameterBuilder> = []

    fun typeParameter(builder: IrTypeParameterBuilder.() -> Unit): IrTypeParameterSymbol {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        val child = IrTypeParameterBuilder().apply(builder)
        typeParameters += child
        return child.symbol
    }

    fun typeParameter(name: String, upperBound: IrType, variance: Variance = Variance.INVARIANT): IrTypeParameterSymbol =
        typeParameter {
            this.name = Name.identifier(name)
            this.superTypes = [upperBound]
            this.variance = variance
        }
}
