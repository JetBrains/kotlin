/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.ir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

/**
 * Collects the properties of a [IrClass] and builds one.
 *
 * A property with no sensible default is declared `lateinit`, so building without it throws rather than
 * inventing a value. There is no way to express "required" for a property the caller assigns inside a
 * lambda; a constructor parameter could, at the cost of the property being settable only once.
 *
 * Built by [IrFactory.build], which is where the factory comes from.
 */
@IrBuilderDsl
class IrClassBuilder {
    var startOffset: Int = UNDEFINED_OFFSET
    var endOffset: Int = UNDEFINED_OFFSET
    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    lateinit var name: Name
    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
    var symbol: IrClassSymbol = IrClassSymbolImpl()
    var kind: ClassKind = ClassKind.CLASS
    var modality: Modality = Modality.FINAL
    var source: SourceElement = SourceElement.NO_SOURCE
    var isExternal: Boolean = false
    var metadata: MetadataSource? = null
    var isCompanion: Boolean = false
    var isInner: Boolean = false
    var isData: Boolean = false
    var isValue: Boolean = false
    var isExpect: Boolean = false
    var isFun: Boolean = false
    var hasEnumEntries: Boolean = false
    val superTypes: MutableList<IrType> = []
    var thisReceiver: IrValueParameter? = null
    var valueClassRepresentation: ValueClassRepresentation<IrSimpleType>? = null
    val sealedSubclasses: MutableList<IrClassSymbol> = []

    /**
     * Takes the source range of [from], the way `IrElement.startOffset` and `endOffset` are usually copied.
     */
    fun setSourceRange(from: IrElement) {
        startOffset = from.startOffset
        endOffset = from.endOffset
    }

    /**
     * Copies the properties of [from] that identify a *kind* of IrClass, not a particular one,
     * including its source range.
     *
     * Its symbol, parent and children are left alone: sharing a symbol between two declarations would bind both
     * to one, and sharing a body or a receiver would splice one tree into another. Properties the element
     * declares `lateinit` are skipped too, since reading one that was never set throws.
     */
    fun updateFrom(from: IrClass) {
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
        name = from.name
        visibility = from.visibility
        kind = from.kind
        modality = from.modality
        source = from.source
        isExternal = from.isExternal
        isCompanion = from.isCompanion
        isInner = from.isInner
        isData = from.isData
        isValue = from.isValue
        isExpect = from.isExpect
        isFun = from.isFun
        hasEnumEntries = from.hasEnumEntries
        valueClassRepresentation = from.valueClassRepresentation
    }
}

/**
 * Builds the collected [IrClass].
 *
 * `declarationCreated` is not decoration: `IrFactoryImplForJsIC` and `IrFactoryImplForWasmIC` override it to
 * attach an `IdSignature` to every declaration they create. Skipping it would leave declarations unsigned, and
 * incremental compilation would cache the wrong thing without failing.
 */
@OptIn(IrImplementationDetail::class)
@PublishedApi
internal fun IrFactory.build(builder: IrClassBuilder): IrClass {
    with(builder) {
        val result = IrClassImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            factory = this@build,
            name = name,
            visibility = visibility,
            symbol = symbol,
            kind = kind,
            modality = modality,
            source = source,
        ).declarationCreated()
        result.isExternal = isExternal
        result.metadata = metadata
        result.isCompanion = isCompanion
        result.isInner = isInner
        result.isData = isData
        result.isValue = isValue
        result.isExpect = isExpect
        result.isFun = isFun
        result.hasEnumEntries = hasEnumEntries
        result.superTypes = superTypes
        result.thisReceiver = thisReceiver
        result.valueClassRepresentation = valueClassRepresentation
        result.sealedSubclasses = sealedSubclasses
        return result
    }
}

@OptIn(ExperimentalContracts::class)
inline fun IrFactory.buildClass(init: IrClassBuilder.() -> Unit): IrClass {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return build(IrClassBuilder().apply(init))
}
