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
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.name.SpecialNames

class IrConstructorBuilder @PublishedApi internal constructor() : IrFunctionBuilder() {
    var isPrimary: Boolean = false

    init {
        name = SpecialNames.INIT
    }

    override fun copyFrom(from: IrDeclaration) {
        super.copyFrom(from)
        if (from is IrConstructor) {
            isPrimary = from.isPrimary
        }
    }
}

@PublishedApi
internal fun IrFactory.build(builder: IrConstructorBuilder): IrConstructor {
    val node = createConstructor(
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
        isPrimary = builder.isPrimary,
        symbol = IrConstructorSymbolImpl(),
    )
    node.annotations = builder.annotations
    node.metadata = builder.metadata
    builder.parent?.let { node.parent = it }
    buildChildren(builder, node)
    return node
}

/**
 * Unless set explicitly, the return type is the class type ([IrClass.defaultType]).
 */
inline fun IrClass.irConstructor(builder: IrConstructorBuilder.() -> Unit = {}): IrConstructor {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildMemberConstructor(IrConstructorBuilder().apply(builder))
}
