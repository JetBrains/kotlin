/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrIndexBasedAttributeRegistry
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

class IrClassImpl @IrImplementationDetail constructor(
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    factory: IrFactory,
    name: Name,
    visibility: DescriptorVisibility,
    symbol: IrClassSymbol,
    kind: ClassKind,
    modality: Modality,
    source: SourceElement,
) : IrClass() {
    override var startOffset: Int by startOffsetAttribute
    override var endOffset: Int by endOffsetAttribute
    override var _attributeOwnerId: IrElement? by _attributeOwnerIdAttribute
    override var annotations: List<IrConstructorCall> by annotationsAttribute
    override var origin: IrDeclarationOrigin by originAttribute
    override val factory: IrFactory by factoryAttribute
    override var name: Name by nameAttribute
    override var isExternal: Boolean by isExternalAttribute
    override var visibility: DescriptorVisibility by visibilityAttribute
    override var typeParameters: List<IrTypeParameter> by typeParametersAttribute
    @UnsafeDuringIrConstructionAPI
    override val declarations: MutableList<IrDeclaration> by declarationsAttribute
    override var metadata: MetadataSource? by metadataAttribute
    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassDescriptor
        get() = symbol.descriptor

    override val symbol: IrClassSymbol by symbolAttribute
    override var kind: ClassKind by kindAttribute
    override var modality: Modality by modalityAttribute
    override var isCompanion: Boolean by isCompanionAttribute
    override var isInner: Boolean by isInnerAttribute
    override var isData: Boolean by isDataAttribute
    override var isValue: Boolean by isValueAttribute
    override var isExpect: Boolean by isExpectAttribute
    override var isFun: Boolean by isFunAttribute
    override var hasEnumEntries: Boolean by hasEnumEntriesAttribute
    override val source: SourceElement by sourceAttribute
    override var superTypes: List<IrType> by superTypesAttribute
    override var thisReceiver: IrValueParameter? by thisReceiverAttribute
    override var valueClassRepresentation: ValueClassRepresentation<IrSimpleType>? by valueClassRepresentationAttribute
    override var sealedSubclasses: List<IrClassSymbol> by sealedSubclassesAttribute

    init {
        preallocateStorage(14)
        initAttribute(startOffsetAttribute, startOffset)
        initAttribute(endOffsetAttribute, endOffset)
        initAttribute(originAttribute, origin)
        initAttribute(factoryAttribute, factory)
        initAttribute(nameAttribute, name)
        initAttribute(kindAttribute, kind)
        initAttribute(visibilityAttribute, visibility)
        initAttribute(declarationsAttribute, ArrayList())
        initAttribute(symbolAttribute, symbol)
        initAttribute(modalityAttribute, modality)
        initAttribute(sourceAttribute, source)

        symbol.bind(this)
    }
    companion object {
        @JvmStatic private val startOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrClassImpl::class.java, 0, "startOffset", null)
        @JvmStatic private val endOffsetAttribute = IrIndexBasedAttributeRegistry.createAttr<Int>(IrClassImpl::class.java, 1, "endOffset", null)
        @JvmStatic private val _attributeOwnerIdAttribute = IrIndexBasedAttributeRegistry.createAttr<IrElement?>(IrClassImpl::class.java, 2, "_attributeOwnerId", null)
        @JvmStatic private val annotationsAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrConstructorCall>>(IrClassImpl::class.java, 3, "annotations", emptyList())
        @JvmStatic private val originAttribute = IrIndexBasedAttributeRegistry.createAttr<IrDeclarationOrigin>(IrClassImpl::class.java, 4, "origin", null)
        @JvmStatic private val factoryAttribute = IrIndexBasedAttributeRegistry.createAttr<IrFactory>(IrClassImpl::class.java, 5, "factory", null)
        @JvmStatic private val nameAttribute = IrIndexBasedAttributeRegistry.createAttr<Name>(IrClassImpl::class.java, 6, "name", null)
        @JvmStatic private val isExternalAttribute = IrIndexBasedAttributeRegistry.createFlag(IrClassImpl::class.java, 63, "isExternal")
        @JvmStatic private val visibilityAttribute = IrIndexBasedAttributeRegistry.createAttr<DescriptorVisibility>(IrClassImpl::class.java, 10, "visibility", null)
        @JvmStatic private val typeParametersAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrTypeParameter>>(IrClassImpl::class.java, 7, "typeParameters", emptyList())
        @JvmStatic private val declarationsAttribute = IrIndexBasedAttributeRegistry.createAttr<MutableList<IrDeclaration>>(IrClassImpl::class.java, 11, "declarations", null)
        @JvmStatic private val metadataAttribute = IrIndexBasedAttributeRegistry.createAttr<MetadataSource?>(IrClassImpl::class.java, 13, "metadata", null)
        @JvmStatic private val symbolAttribute = IrIndexBasedAttributeRegistry.createAttr<IrClassSymbol>(IrClassImpl::class.java, 12, "symbol", null)
        @JvmStatic private val kindAttribute = IrIndexBasedAttributeRegistry.createAttr<ClassKind>(IrClassImpl::class.java, 9, "kind", null)
        @JvmStatic private val modalityAttribute = IrIndexBasedAttributeRegistry.createAttr<Modality>(IrClassImpl::class.java, 14, "modality", null)
        @JvmStatic private val isCompanionAttribute = IrIndexBasedAttributeRegistry.createFlag(IrClassImpl::class.java, 62, "isCompanion")
        @JvmStatic private val isInnerAttribute = IrIndexBasedAttributeRegistry.createFlag(IrClassImpl::class.java, 61, "isInner")
        @JvmStatic private val isDataAttribute = IrIndexBasedAttributeRegistry.createFlag(IrClassImpl::class.java, 60, "isData")
        @JvmStatic private val isValueAttribute = IrIndexBasedAttributeRegistry.createFlag(IrClassImpl::class.java, 59, "isValue")
        @JvmStatic private val isExpectAttribute = IrIndexBasedAttributeRegistry.createFlag(IrClassImpl::class.java, 58, "isExpect")
        @JvmStatic private val isFunAttribute = IrIndexBasedAttributeRegistry.createFlag(IrClassImpl::class.java, 57, "isFun")
        @JvmStatic private val hasEnumEntriesAttribute = IrIndexBasedAttributeRegistry.createFlag(IrClassImpl::class.java, 56, "hasEnumEntries")
        @JvmStatic private val sourceAttribute = IrIndexBasedAttributeRegistry.createAttr<SourceElement>(IrClassImpl::class.java, 15, "source", null)
        @JvmStatic private val superTypesAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrType>>(IrClassImpl::class.java, 16, "superTypes", emptyList())
        @JvmStatic private val thisReceiverAttribute = IrIndexBasedAttributeRegistry.createAttr<IrValueParameter?>(IrClassImpl::class.java, 17, "thisReceiver", null)
        @JvmStatic private val valueClassRepresentationAttribute = IrIndexBasedAttributeRegistry.createAttr<ValueClassRepresentation<IrSimpleType>?>(IrClassImpl::class.java, 18, "valueClassRepresentation", null)
        @JvmStatic private val sealedSubclassesAttribute = IrIndexBasedAttributeRegistry.createAttr<List<IrClassSymbol>>(IrClassImpl::class.java, 19, "sealedSubclasses", emptyList())
    }
}
