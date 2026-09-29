/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.ir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

/**
 * Collects the properties of a [IrField] and builds one.
 *
 * A property with no sensible default is declared `lateinit`, so building without it throws rather than
 * inventing a value. There is no way to express "required" for a property the caller assigns inside a
 * lambda; a constructor parameter could, at the cost of the property being settable only once.
 *
 * Built by [IrFactory.build], which is where the factory comes from.
 */
@IrBuilderDsl
class IrFieldBuilder {
    var startOffset: Int = UNDEFINED_OFFSET
    var endOffset: Int = UNDEFINED_OFFSET
    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    lateinit var name: Name
    var isExternal: Boolean = false
    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
    var symbol: IrFieldSymbol = IrFieldSymbolImpl()
    lateinit var type: IrType
    var isFinal: Boolean = false
    var isStatic: Boolean = false
    var metadata: MetadataSource? = null
    var initializer: IrExpressionBody? = null
    var correspondingPropertySymbol: IrPropertySymbol? = null

    /**
     * Takes the source range of [from], the way `IrElement.startOffset` and `endOffset` are usually copied.
     */
    fun setSourceRange(from: IrElement) {
        startOffset = from.startOffset
        endOffset = from.endOffset
    }

    /**
     * Copies the properties of [from] that identify a *kind* of IrField, not a particular one,
     * including its source range.
     *
     * Its symbol, parent and children are left alone: sharing a symbol between two declarations would bind both
     * to one, and sharing a body or a receiver would splice one tree into another. Properties the element
     * declares `lateinit` are skipped too, since reading one that was never set throws.
     */
    fun updateFrom(from: IrField) {
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
        name = from.name
        isExternal = from.isExternal
        visibility = from.visibility
        type = from.type
        isFinal = from.isFinal
        isStatic = from.isStatic
        metadata = from.metadata
    }
}

/**
 * Builds the collected [IrField].
 *
 * `declarationCreated` is not decoration: `IrFactoryImplForJsIC` and `IrFactoryImplForWasmIC` override it to
 * attach an `IdSignature` to every declaration they create. Skipping it would leave declarations unsigned, and
 * incremental compilation would cache the wrong thing without failing.
 */
@OptIn(IrImplementationDetail::class)
@PublishedApi
internal fun IrFactory.build(builder: IrFieldBuilder): IrField {
    with(builder) {
        val result = IrFieldImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            factory = this@build,
            name = name,
            isExternal = isExternal,
            visibility = visibility,
            symbol = symbol,
            type = type,
            isFinal = isFinal,
            isStatic = isStatic,
        ).declarationCreated()
        result.metadata = metadata
        result.initializer = initializer
        result.correspondingPropertySymbol = correspondingPropertySymbol
        return result
    }
}

@OptIn(ExperimentalContracts::class)
inline fun IrFactory.buildField(init: IrFieldBuilder.() -> Unit): IrField {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return build(IrFieldBuilder().apply(init))
}
