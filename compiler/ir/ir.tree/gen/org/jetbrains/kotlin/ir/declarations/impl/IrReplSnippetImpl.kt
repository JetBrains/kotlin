/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrReplSnippetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrReplSnippetImpl(
    override var startOffset: Int,
    override var endOffset: Int,
    override val factory: IrFactory,
    override var name: Name,
    override val symbol: IrReplSnippetSymbol,
) : IrReplSnippet() {
    override var attributeOwnerId: IrElement = this

    override var annotations: List<IrConstructorCall> = emptyList()

    @ObsoleteDescriptorBasedAPI
    override val descriptor: DeclarationDescriptor
        get() = symbol.descriptor

    override var origin: IrDeclarationOrigin = REPL_SNIPPET_ORIGIN

    override var metadata: MetadataSource? = null

    override lateinit var receiverParameters: List<IrValueParameter>

    override val variablesFromOtherSnippets: MutableList<IrVariable> = ArrayList()

    override val declarationsFromOtherSnippets: MutableList<IrDeclaration> = ArrayList()

    override var stateObject: IrClassSymbol? = null

    override lateinit var body: IrBody

    override var returnType: IrType? = null

    override var targetClass: IrClassSymbol? = null

    init {
        symbol.bind(this)
    }
}
