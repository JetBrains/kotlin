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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType

class IrClassBuilder @PublishedApi internal constructor() : IrTypeParametersContainerBuilder() {
    var isExternal: Boolean = false

    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    var metadata: MetadataSource? = null

    var kind: ClassKind = ClassKind.CLASS

    var modality: Modality = Modality.FINAL

    var isCompanion: Boolean = false

    var isInner: Boolean = false

    var isData: Boolean = false

    var isValue: Boolean = false

    var isExpect: Boolean = false

    var isFun: Boolean = false

    var hasEnumEntries: Boolean = false

    var source: SourceElement = SourceElement.NO_SOURCE

    var superTypes: List<IrType> = []

    var valueClassRepresentation: ValueClassRepresentation<IrSimpleType>? = null

    var sealedSubclasses: List<IrClassSymbol> = []

    internal var thisReceiver: IrDispatchReceiverBuilder? = null
    internal val declarations: MutableList<IrDeclaration> = []

    override fun updateFrom(from: IrDeclaration) {
        super.updateFrom(from)
        if (from is IrClass) {
            isExternal = from.isExternal
            visibility = from.visibility
            kind = from.kind
            modality = from.modality
            isCompanion = from.isCompanion
            isInner = from.isInner
            isData = from.isData
            isValue = from.isValue
            isExpect = from.isExpect
            isFun = from.isFun
            hasEnumEntries = from.hasEnumEntries
            source = from.source
            superTypes = from.superTypes
            valueClassRepresentation = from.valueClassRepresentation
        }
    }

    /**
     * Unless set explicitly, offsets, origin ([org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.INSTANCE_RECEIVER])
     * and type (the class type with its type parameters) are taken from the class when it's built.
     */
    fun thisReceiver(builder: IrDispatchReceiverBuilder.() -> Unit = {}) {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        val child = IrDispatchReceiverBuilder().apply(builder)
        thisReceiver = child
    }

    fun declaration(declaration: IrDeclaration) {
        declarations += declaration
    }
}

@PublishedApi
internal fun IrFactory.build(builder: IrClassBuilder): IrClass {
    val node = createClass(
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        origin = builder.origin,
        name = builder.name,
        visibility = builder.visibility,
        kind = builder.kind,
        modality = builder.modality,
        source = builder.source,
        symbol = IrClassSymbolImpl(),
    )
    node.annotations = builder.annotations
    node.isExternal = builder.isExternal
    node.metadata = builder.metadata
    node.isCompanion = builder.isCompanion
    node.isInner = builder.isInner
    node.isData = builder.isData
    node.isValue = builder.isValue
    node.isExpect = builder.isExpect
    node.isFun = builder.isFun
    node.hasEnumEntries = builder.hasEnumEntries
    node.superTypes = builder.superTypes
    node.valueClassRepresentation = builder.valueClassRepresentation
    node.sealedSubclasses = builder.sealedSubclasses
    builder.parent?.let { node.parent = it }
    buildChildren(builder, node)
    return node
}

inline fun IrFactory.irClass(builder: IrClassBuilder.() -> Unit): IrClass {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrClassBuilder().apply(builder))
}
