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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl

class IrSimpleFunctionBuilder @PublishedApi internal constructor() : IrFunctionBuilder() {
    var modality: Modality = Modality.FINAL

    var isFakeOverride: Boolean = false

    var overriddenSymbols: List<IrSimpleFunctionSymbol> = []

    var isTailrec: Boolean = false

    var isSuspend: Boolean = false

    var isOperator: Boolean = false

    var isInfix: Boolean = false

    var companionExtensionClass: IrClassSymbol? = null

    internal val symbol: IrSimpleFunctionSymbol = IrSimpleFunctionSymbolImpl()

    override fun copyFrom(from: IrDeclaration) {
        super.copyFrom(from)
        if (from is IrSimpleFunction) {
            modality = from.modality
            isFakeOverride = from.isFakeOverride
            isTailrec = from.isTailrec
            isSuspend = from.isSuspend
            isOperator = from.isOperator
            isInfix = from.isInfix
        }
    }
}

@PublishedApi
internal fun IrFactory.build(builder: IrSimpleFunctionBuilder): IrSimpleFunction {
    val node = createSimpleFunction(
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        origin = builder.origin,
        name = builder.name,
        isExternal = builder.isExternal,
        visibility = builder.visibility,
        containerSource = builder.containerSource,
        isInline = builder.isInline,
        isExpect = builder.isExpect,
        returnType = builder.returnType,
        modality = builder.modality,
        isFakeOverride = builder.isFakeOverride,
        isTailrec = builder.isTailrec,
        isSuspend = builder.isSuspend,
        isOperator = builder.isOperator,
        isInfix = builder.isInfix,
        companionExtensionClass = builder.companionExtensionClass,
        symbol = builder.symbol,
    )
    node.annotations = builder.annotations
    node.metadata = builder.metadata
    node.overriddenSymbols = builder.overriddenSymbols
    builder.parent?.let { node.parent = it }
    buildChildren(builder, node)
    return node
}

inline fun IrFactory.irFunction(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrSimpleFunctionBuilder().apply(builder))
}

/**
 * A member function. Unless the builder declares a dispatch receiver explicitly, it gets a copy of the class [IrClass.thisReceiver].
 */
inline fun IrClass.irFunction(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildMemberFunction(IrSimpleFunctionBuilder().apply(builder))
}

inline fun IrClass.irStaticFunction(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildStaticFunction(IrSimpleFunctionBuilder().apply(builder))
}

inline fun IrProperty.irGetter(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildGetter(IrSimpleFunctionBuilder().apply(builder))
}

inline fun IrProperty.irSetter(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunction {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildSetter(IrSimpleFunctionBuilder().apply(builder))
}
