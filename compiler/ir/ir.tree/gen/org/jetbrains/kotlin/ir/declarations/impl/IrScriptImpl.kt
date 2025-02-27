/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.ScriptDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrScriptImpl(
    symbol: IrScriptSymbol,
    name: Name,
    factory: IrFactory,
    startOffset: Int,
    endOffset: Int,
) : IrScript() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    override val statements: MutableList<IrStatement> by statementsAttribute
    override var metadata: MetadataSource? by metadataAttribute
    override val symbol: IrScriptSymbol by symbolAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ScriptDescriptor
        get() = symbol.descriptor

    override var thisReceiver: IrValueParameter? by thisReceiverAttribute
    override var baseClass: IrType? by baseClassAttribute
    override var explicitCallParameters: List<IrVariable> by explicitCallParametersAttribute
    override var implicitReceiversParameters: List<IrValueParameter> by implicitReceiversParametersAttribute
    override var providedProperties: List<IrPropertySymbol> by providedPropertiesAttribute
    override var providedPropertiesParameters: List<IrValueParameter> by providedPropertiesParametersAttribute
    override var resultProperty: IrPropertySymbol? by resultPropertyAttribute
    override var earlierScriptsParameter: IrValueParameter? by earlierScriptsParameterAttribute
    override var importedScripts: List<IrScriptSymbol>? by importedScriptsAttribute
    override var earlierScripts: List<IrScriptSymbol>? by earlierScriptsAttribute
    override var targetClass: IrClassSymbol? by targetClassAttribute
    override var constructor: IrConstructor? by constructorAttribute

    init {
        preallocateStorage(14)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(statementsAttribute, ArrayList(2))
        initAttribute(symbolAttribute, symbol)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrScriptImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrScriptImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrScriptImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrScriptImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrScriptImpl::class.java, 4, "origin", SCRIPT_ORIGIN)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrScriptImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrScriptImpl::class.java, 6, "name", null)
        @JvmStatic private val statementsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrStatement>>(IrScriptImpl::class.java, 9, "statements", null)
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrScriptImpl::class.java, 13, "metadata", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrScriptSymbol>(IrScriptImpl::class.java, 12, "symbol", null)
        @JvmStatic private val thisReceiverAttribute = IrIndexBasedAttributeRegistry.createAttr<IrValueParameter?>(IrScriptImpl::class.java, 17, "thisReceiver", null)
        @JvmStatic private val baseClassAttribute = IrIndexBasedAttributeRegistry.createAttr<IrType?>(IrScriptImpl::class.java, 7, "baseClass", null)
        @JvmStatic private val explicitCallParametersAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrVariable>>(IrScriptImpl::class.java, 10, "explicitCallParameters", null)
        @JvmStatic private val implicitReceiversParametersAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrValueParameter>>(IrScriptImpl::class.java, 11, "implicitReceiversParameters", null)
        @JvmStatic private val providedPropertiesAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrPropertySymbol>>(IrScriptImpl::class.java, 14, "providedProperties", null)
        @JvmStatic private val providedPropertiesParametersAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrValueParameter>>(IrScriptImpl::class.java, 15, "providedPropertiesParameters", null)
        @JvmStatic private val resultPropertyAttribute = IrIndexBasedAttributeRegistry.createAttr<IrPropertySymbol?>(IrScriptImpl::class.java, 16, "resultProperty", null)
        @JvmStatic private val earlierScriptsParameterAttribute = IrIndexBasedAttributeRegistry.createAttr<IrValueParameter?>(IrScriptImpl::class.java, 18, "earlierScriptsParameter", null)
        @JvmStatic private val importedScriptsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrScriptSymbol>?>(IrScriptImpl::class.java, 19, "importedScripts", null)
        @JvmStatic private val earlierScriptsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrScriptSymbol>?>(IrScriptImpl::class.java, 20, "earlierScripts", null)
        @JvmStatic private val targetClassAttribute = IrIndexBasedAttributeRegistry.createAttr<IrClassSymbol?>(IrScriptImpl::class.java, 21, "targetClass", null)
        @JvmStatic private val constructorAttribute = IrIndexBasedAttributeRegistry.createAttr<IrConstructor?>(IrScriptImpl::class.java, 22, "constructor", null)
    }
}
