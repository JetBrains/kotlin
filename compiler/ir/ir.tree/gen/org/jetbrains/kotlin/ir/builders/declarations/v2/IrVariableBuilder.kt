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
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType

/**
 * Unless set explicitly, the type is the type of the initializer.
 */
class IrVariableBuilder @PublishedApi internal constructor() : IrDeclarationWithNameBuilder() {
    lateinit var type: IrType

    var isVar: Boolean = false

    var isConst: Boolean = false

    var isLateinit: Boolean = false

    var initializer: IrExpression? = null

    internal val isTypeInitialized: Boolean
        get() = ::type.isInitialized

    override fun copyFrom(from: IrDeclaration) {
        super.copyFrom(from)
        if (from is IrVariable) {
            type = from.type
            isVar = from.isVar
            isConst = from.isConst
            isLateinit = from.isLateinit
        }
    }
}

@PublishedApi
internal fun IrFactory.build(builder: IrVariableBuilder): IrVariable {
    applyDefaults(builder)
    val node = IrVariableImpl(
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        origin = builder.origin,
        name = builder.name,
        type = builder.type,
        isVar = builder.isVar,
        isConst = builder.isConst,
        isLateinit = builder.isLateinit,
        symbol = IrVariableSymbolImpl(),
    )
    node.annotations = builder.annotations
    node.initializer = builder.initializer
    builder.parent?.let { node.parent = it }
    return node
}

inline fun IrFactory.irVariable(builder: IrVariableBuilder.() -> Unit): IrVariable {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrVariableBuilder().apply(builder))
}

inline fun IrBuilderWithScope.irVariable(builder: IrVariableBuilder.() -> Unit): IrVariable {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildLocalVariable(localVariableBuilder().apply(builder))
}

@PublishedApi
internal fun IrBuilderWithScope.localVariableBuilder(): IrVariableBuilder =
    IrVariableBuilder().also {
        it.startOffset = startOffset
        it.endOffset = endOffset
    }
