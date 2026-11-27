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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class IrPropertyBuilder @PublishedApi internal constructor() : IrDeclarationWithNameBuilder() {
    var isExternal: Boolean = false

    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC

    var modality: Modality = Modality.FINAL

    var isFakeOverride: Boolean = false

    var metadata: MetadataSource? = null

    var containerSource: DeserializedContainerSource? = null

    var overriddenSymbols: List<IrPropertySymbol> = []

    var isVar: Boolean = false

    var isConst: Boolean = false

    var isLateinit: Boolean = false

    var isDelegated: Boolean = false

    var isExpect: Boolean = false

    internal var backingField: IrFieldBuilder? = null
    internal var getter: IrSimpleFunctionBuilder? = null
    internal var setter: IrSimpleFunctionBuilder? = null

    override fun copyFrom(from: IrDeclaration) {
        super.copyFrom(from)
        if (from is IrProperty) {
            isExternal = from.isExternal
            visibility = from.visibility
            modality = from.modality
            isFakeOverride = from.isFakeOverride
            containerSource = from.containerSource
            isVar = from.isVar
            isConst = from.isConst
            isLateinit = from.isLateinit
            isDelegated = from.isDelegated
            isExpect = from.isExpect
        }
    }

    /**
     * Unless set explicitly, the name is the name of the property.
     */
    fun backingField(builder: IrFieldBuilder.() -> Unit): IrFieldSymbol {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        check(backingField == null) { "The backing field is already declared" }
        val child = backingFieldBuilder().apply(builder)
        backingField = child
        return child.symbol
    }

    /**
     * Unless set explicitly, the name is `<get-name>`.
     */
    fun getter(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunctionSymbol {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        check(getter == null) { "The getter is already declared" }
        val child = IrSimpleFunctionBuilder().apply(builder)
        getter = child
        return child.symbol
    }

    /**
     * Unless set explicitly, the name is `<set-name>`. The value parameter isn't added implicitly.
     */
    fun setter(builder: IrSimpleFunctionBuilder.() -> Unit): IrSimpleFunctionSymbol {
        contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
        check(setter == null) { "The setter is already declared" }
        val child = IrSimpleFunctionBuilder().apply(builder)
        setter = child
        return child.symbol
    }
}

@PublishedApi
internal fun IrFactory.build(builder: IrPropertyBuilder): IrProperty {
    val node = createProperty(
        startOffset = builder.startOffset,
        endOffset = builder.endOffset,
        origin = builder.origin,
        name = builder.name,
        isExternal = builder.isExternal,
        visibility = builder.visibility,
        modality = builder.modality,
        isFakeOverride = builder.isFakeOverride,
        containerSource = builder.containerSource,
        isVar = builder.isVar,
        isConst = builder.isConst,
        isLateinit = builder.isLateinit,
        isDelegated = builder.isDelegated,
        isExpect = builder.isExpect,
        symbol = IrPropertySymbolImpl(),
    )
    node.annotations = builder.annotations
    node.metadata = builder.metadata
    node.overriddenSymbols = builder.overriddenSymbols
    builder.parent?.let { node.parent = it }
    buildChildren(builder, node)
    return node
}

inline fun IrFactory.irProperty(builder: IrPropertyBuilder.() -> Unit): IrProperty {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return build(IrPropertyBuilder().apply(builder))
}

/**
 * A member property: its accessors get dispatch receivers the same way as [irFunction] does.
 */
inline fun IrClass.irProperty(builder: IrPropertyBuilder.() -> Unit): IrProperty {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildMemberProperty(IrPropertyBuilder().apply(builder))
}

/**
 * A property with accessors without dispatch receivers, see [irStaticFunction]. The backing field must be marked static explicitly.
 */
inline fun IrClass.irStaticProperty(builder: IrPropertyBuilder.() -> Unit): IrProperty {
    contract { callsInPlace(builder, InvocationKind.EXACTLY_ONCE) }
    return buildStaticProperty(IrPropertyBuilder().apply(builder))
}
