/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrAnonymousInitializerSymbol

class IrAnonymousInitializerImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    symbol: IrAnonymousInitializerSymbol,
    isStatic: Boolean,
) : IrAnonymousInitializer() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override val symbol: IrAnonymousInitializerSymbol by symbolAttribute
    override var isStatic: Boolean by isStaticAttribute
    override var body: IrBlockBody by bodyAttribute

    init {
        preallocateStorage(7)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(symbolAttribute, symbol)
        if (isStatic) setFlagInternal(isStaticAttribute, true)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrAnonymousInitializerImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrAnonymousInitializerImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrAnonymousInitializerImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrAnonymousInitializerImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrAnonymousInitializerImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrAnonymousInitializerImpl::class.java, 5, "factory", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrAnonymousInitializerSymbol>(IrAnonymousInitializerImpl::class.java, 12, "symbol", null)
        @JvmStatic private val isStaticAttribute = IrIndexBasedAttributeRegistry.createFlag(IrAnonymousInitializerImpl::class.java, 62, "isStatic")
        @JvmStatic private val bodyAttribute = IrIndexBasedAttributeRegistry.createAttr<IrBlockBody>(IrAnonymousInitializerImpl::class.java, 9, "body", null)
    }
}
