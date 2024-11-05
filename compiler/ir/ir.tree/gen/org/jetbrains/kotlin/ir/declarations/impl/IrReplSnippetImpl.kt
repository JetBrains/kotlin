/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ReplSnippetDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrReplSnippetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrReplSnippetImpl(
    override val symbol: IrReplSnippetSymbol,
    override var name: Name,
    override val factory: IrFactory,
    override val startOffset: Int,
    override val endOffset: Int,
) : IrReplSnippet() {
    override var annotations: List<IrConstructorCall> = emptyList()

    override var origin: IrDeclarationOrigin = REPL_SNIPPET_ORIGIN

    override var metadata: MetadataSource? = null

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ReplSnippetDescriptor
        get() = symbol.descriptor

    override lateinit var receiversParameters: List<IrValueParameter>

    override val variablesFromOtherSnippets: MutableList<IrVariable> = ArrayList()

    override val capturingDeclarationsFromOtherSnippets: MutableList<IrDeclaration> = ArrayList()

    override var stateObject: IrClassSymbol? = null

    override lateinit var body: IrBody

    override var returnType: IrType? = null

    override var targetClass: IrClassSymbol? = null

    init {
        symbol.bind(this)
    }
}
