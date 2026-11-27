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
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl

class IrAnonymousInitializerBuilder @PublishedApi internal constructor() : IrDeclarationBuilder() {
    var isStatic: Boolean = false

    override fun updateFrom(from: IrDeclaration) {
        super.updateFrom(from)
        if (from is IrAnonymousInitializer) {
            isStatic = from.isStatic
        }
    }
}

@PublishedApi
internal fun IrFactory.build(builder: IrAnonymousInitializerBuilder): IrAnonymousInitializer {
    val node = createAnonymousInitializer(
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        origin = builder.origin,
        isStatic = builder.isStatic,
        symbol = IrAnonymousInitializerSymbolImpl(),
    )
    node.annotations = builder.annotations
    builder.parent?.let { node.parent = it }
    return node
}

inline fun IrClass.irAnonymousInitializer(builder: IrAnonymousInitializerBuilder.() -> Unit = {}): IrAnonymousInitializer {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildMemberAnonymousInitializer(IrAnonymousInitializerBuilder().apply(builder))
}
