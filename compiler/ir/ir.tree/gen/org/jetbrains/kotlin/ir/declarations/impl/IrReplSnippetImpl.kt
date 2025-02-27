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
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrReplSnippetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrReplSnippetImpl(
    startOffset: Int,
    endOffset: Int,
    factory: IrFactory,
    name: Name,
    symbol: IrReplSnippetSymbol,
) : IrReplSnippet() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: DeclarationDescriptor
        get() = symbol.descriptor

    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    override var metadata: MetadataSource? by metadataAttribute
    override val symbol: IrReplSnippetSymbol by symbolAttribute
    override var receiverParameters: List<IrValueParameter> by receiverParametersAttribute
    override val variablesFromOtherSnippets: MutableList<IrVariable> by variablesFromOtherSnippetsAttribute
    override val declarationsFromOtherSnippets: MutableList<IrDeclaration> by declarationsFromOtherSnippetsAttribute
    override var stateObject: IrClassSymbol? by stateObjectAttribute
    override var body: IrBody by bodyAttribute
    override var returnType: IrType? by returnTypeAttribute
    override var targetClass: IrClassSymbol? by targetClassAttribute

    init {
        preallocateStorage(11)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(variablesFromOtherSnippetsAttribute, ArrayList())
        initAttribute(declarationsFromOtherSnippetsAttribute, ArrayList())
        initAttribute(symbolAttribute, symbol)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrReplSnippetImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrReplSnippetImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrReplSnippetImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrReplSnippetImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrReplSnippetImpl::class.java, 4, "origin", REPL_SNIPPET_ORIGIN)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrReplSnippetImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrReplSnippetImpl::class.java, 6, "name", null)
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrReplSnippetImpl::class.java, 13, "metadata", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrReplSnippetSymbol>(IrReplSnippetImpl::class.java, 12, "symbol", null)
        @JvmStatic private val receiverParametersAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrValueParameter>>(IrReplSnippetImpl::class.java, 7, "receiverParameters", null)
        @JvmStatic private val variablesFromOtherSnippetsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrVariable>>(IrReplSnippetImpl::class.java, 10, "variablesFromOtherSnippets", null)
        @JvmStatic private val declarationsFromOtherSnippetsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrDeclaration>>(IrReplSnippetImpl::class.java, 11, "declarationsFromOtherSnippets", null)
        @JvmStatic private val stateObjectAttribute = IrIndexBasedAttributeRegistry.createAttr<IrClassSymbol?>(IrReplSnippetImpl::class.java, 14, "stateObject", null)
        @JvmStatic private val bodyAttribute = IrIndexBasedAttributeRegistry.createAttr<IrBody>(IrReplSnippetImpl::class.java, 9, "body", null)
        @JvmStatic private val returnTypeAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType?>(IrReplSnippetImpl::class.java, 15, "returnType", null)
        @JvmStatic private val targetClassAttribute = IrIndexBasedAttributeRegistry.createAttr<IrClassSymbol?>(IrReplSnippetImpl::class.java, 21, "targetClass", null)
    }
}
