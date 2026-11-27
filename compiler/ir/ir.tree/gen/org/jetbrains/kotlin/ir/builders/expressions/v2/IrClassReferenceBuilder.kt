/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:JvmName("IrExpressionBuilders")
@file:JvmMultifileClass
@file:OptIn(ExperimentalContracts::class)

package org.jetbrains.kotlin.ir.builders.expressions.v2

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType

class IrClassReferenceBuilder @PublishedApi internal constructor(
    override val symbol: IrClassifierSymbol,
) : IrDeclarationReferenceBuilder() {
    lateinit var classType: IrType
}

@PublishedApi
internal fun build(builder: IrClassReferenceBuilder): IrClassReference {
    val node = IrClassReferenceImpl(
        constructorIndicator = null,
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        type = builder.type,
        symbol = builder.symbol,
        classType = builder.classType,
    )
    return node
}

inline fun buildClassReference(symbol: IrClassifierSymbol, builder: IrClassReferenceBuilder.() -> Unit): IrClassReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrClassReferenceBuilder(symbol).apply(builder))
}

inline fun IrBuilder.irClassReference(symbol: IrClassifierSymbol, builder: IrClassReferenceBuilder.() -> Unit): IrClassReference {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(classReferenceBuilder(symbol).apply(builder))
}

@PublishedApi
internal fun IrBuilder.classReferenceBuilder(symbol: IrClassifierSymbol): IrClassReferenceBuilder =
    IrClassReferenceBuilder(symbol).also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
