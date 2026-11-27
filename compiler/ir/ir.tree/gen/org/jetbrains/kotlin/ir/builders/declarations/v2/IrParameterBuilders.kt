/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:JvmName("IrDeclarationBuilders")
@file:JvmMultifileClass

package org.jetbrains.kotlin.ir.builders.declarations.v2

import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.SpecialNames

abstract class IrParameterBuilder internal constructor() : IrDeclarationWithNameBuilder() {
    lateinit var type: IrType

    abstract val kind: IrParameterKind

    var isAssignable: Boolean = false

    var varargElementType: IrType? = null

    var isCrossinline: Boolean = false

    var isNoinline: Boolean = false

    var isHidden: Boolean = false

    internal val symbol: IrValueParameterSymbol = IrValueParameterSymbolImpl()

    internal val isTypeInitialized: Boolean
        get() = ::type.isInitialized

    /**
     * Copies the header of [from] as is, without relations to other declarations, metadata, annotations and children.
     */
    fun copyFrom(from: IrValueParameter) {
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
        name = from.name
        type = from.type
        isAssignable = from.isAssignable
        varargElementType = from.varargElementType
        isCrossinline = from.isCrossinline
        isNoinline = from.isNoinline
        isHidden = from.isHidden
    }
}

internal fun IrFactory.build(builder: IrParameterBuilder): IrValueParameter {
    val node = createValueParameter(
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        origin = builder.origin,
        name = builder.name,
        type = builder.type,
        kind = builder.kind,
        isAssignable = builder.isAssignable,
        varargElementType = builder.varargElementType,
        isCrossinline = builder.isCrossinline,
        isNoinline = builder.isNoinline,
        isHidden = builder.isHidden,
        symbol = builder.symbol,
    )
    node.annotations = builder.annotations
    builder.parent?.let { node.parent = it }
    return node
}

class IrValueParameterBuilder internal constructor() : IrParameterBuilder() {
    override var kind: IrParameterKind = IrParameterKind.Regular
}

class IrRegularParameterBuilder internal constructor() : IrParameterBuilder() {
    override val kind: IrParameterKind
        get() = IrParameterKind.Regular
}

class IrDispatchReceiverBuilder internal constructor() : IrParameterBuilder() {
    override val kind: IrParameterKind
        get() = IrParameterKind.DispatchReceiver

    init {
        name = SpecialNames.THIS
    }
}

class IrExtensionReceiverBuilder internal constructor() : IrParameterBuilder() {
    override val kind: IrParameterKind
        get() = IrParameterKind.ExtensionReceiver

    init {
        name = SpecialNames.THIS
    }
}

class IrContextParameterBuilder internal constructor() : IrParameterBuilder() {
    override val kind: IrParameterKind
        get() = IrParameterKind.Context
}
