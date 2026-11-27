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
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType

class IrFieldBuilder @PublishedApi internal constructor() : IrDeclarationWithNameBuilder() {
    var isExternal: Boolean = false

    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    var metadata: MetadataSource? = null

    lateinit var type: IrType

    var isFinal: Boolean = false

    var isStatic: Boolean = false

    internal val symbol: IrFieldSymbol = IrFieldSymbolImpl()

    /**
     * Copies the header of [from] as is, without relations to other declarations, metadata, annotations and children.
     */
    fun copyFrom(from: IrField) {
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
        name = from.name
        isExternal = from.isExternal
        visibility = from.visibility
        type = from.type
        isFinal = from.isFinal
        isStatic = from.isStatic
    }
}

@PublishedApi
internal fun IrFactory.build(builder: IrFieldBuilder): IrField {
    val node = createField(
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        origin = builder.origin,
        name = builder.name,
        isExternal = builder.isExternal,
        visibility = builder.visibility,
        type = builder.type,
        isFinal = builder.isFinal,
        isStatic = builder.isStatic,
        symbol = builder.symbol,
    )
    node.annotations = builder.annotations
    node.metadata = builder.metadata
    builder.parent?.let { node.parent = it }
    return node
}

inline fun IrFactory.irField(builder: IrFieldBuilder.() -> Unit): IrField {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrFieldBuilder().apply(builder))
}

inline fun IrClass.irField(builder: IrFieldBuilder.() -> Unit): IrField {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildMemberField(IrFieldBuilder().apply(builder))
}

inline fun IrProperty.irBackingField(builder: IrFieldBuilder.() -> Unit): IrField {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildBackingField(backingFieldBuilder().apply(builder))
}

@PublishedApi
internal fun backingFieldBuilder(): IrFieldBuilder =
    IrFieldBuilder().also {
        it.origin = IrDeclarationOrigin.PROPERTY_BACKING_FIELD
        it.visibility = DescriptorVisibilities.PRIVATE
    }
