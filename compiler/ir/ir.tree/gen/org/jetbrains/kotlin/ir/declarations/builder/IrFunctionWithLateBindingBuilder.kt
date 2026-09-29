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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunctionWithLateBinding
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionWithLateBindingImpl
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

/**
 * Collects the properties of a [IrFunctionWithLateBinding] and builds one.
 *
 * A property with no sensible default is declared `lateinit`, so building without it throws rather than
 * inventing a value. There is no way to express "required" for a property the caller assigns inside a
 * lambda; a constructor parameter could, at the cost of the property being settable only once.
 *
 * Built by [IrFactory.build], which is where the factory comes from.
 */
@IrBuilderDsl
class IrFunctionWithLateBindingBuilder {
    var startOffset: Int = UNDEFINED_OFFSET
    var endOffset: Int = UNDEFINED_OFFSET
    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    lateinit var name: Name
    var isExternal: Boolean = false
    var visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
    var isInline: Boolean = false
    var isExpect: Boolean = false
    var modality: Modality = Modality.FINAL
    var isFakeOverride: Boolean = false
    var isTailrec: Boolean = false
    var isSuspend: Boolean = false
    var isOperator: Boolean = false
    var isInfix: Boolean = false
    var companionExtensionClass: IrClassSymbol? = null
    var metadata: MetadataSource? = null
    var returnType: IrType? = null
    var body: IrBody? = null
    val overriddenSymbols: MutableList<IrSimpleFunctionSymbol> = []
    var correspondingPropertySymbol: IrPropertySymbol? = null

    /**
     * Takes the source range of [from], the way `IrElement.startOffset` and `endOffset` are usually copied.
     */
    fun setSourceRange(from: IrElement) {
        startOffset = from.startOffset
        endOffset = from.endOffset
    }

    /**
     * Copies the properties of [from] that identify a *kind* of IrFunctionWithLateBinding, not a particular one,
     * including its source range.
     *
     * Its symbol, parent and children are left alone: sharing a symbol between two declarations would bind both
     * to one, and sharing a body or a receiver would splice one tree into another. Properties the element
     * declares `lateinit` are skipped too, since reading one that was never set throws.
     */
    fun updateFrom(from: IrFunctionWithLateBinding) {
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
        name = from.name
        isExternal = from.isExternal
        visibility = from.visibility
        isInline = from.isInline
        isExpect = from.isExpect
        modality = from.modality
        isFakeOverride = from.isFakeOverride
        isTailrec = from.isTailrec
        isSuspend = from.isSuspend
        isOperator = from.isOperator
        isInfix = from.isInfix
    }
}

/**
 * Builds the collected [IrFunctionWithLateBinding].
 *
 * `declarationCreated` is not decoration: `IrFactoryImplForJsIC` and `IrFactoryImplForWasmIC` override it to
 * attach an `IdSignature` to every declaration they create. Skipping it would leave declarations unsigned, and
 * incremental compilation would cache the wrong thing without failing.
 */
@OptIn(IrImplementationDetail::class)
@PublishedApi
internal fun IrFactory.build(builder: IrFunctionWithLateBindingBuilder): IrFunctionWithLateBinding {
    with(builder) {
        val result = IrFunctionWithLateBindingImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            factory = this@build,
            name = name,
            isExternal = isExternal,
            visibility = visibility,
            isInline = isInline,
            isExpect = isExpect,
            modality = modality,
            isFakeOverride = isFakeOverride,
            isTailrec = isTailrec,
            isSuspend = isSuspend,
            isOperator = isOperator,
            isInfix = isInfix,
            companionExtensionClass = companionExtensionClass,
        ).declarationCreated()
        result.metadata = metadata
        returnType?.let { result.returnType = it }
        result.body = body
        result.overriddenSymbols = overriddenSymbols
        result.correspondingPropertySymbol = correspondingPropertySymbol
        return result
    }
}

@OptIn(ExperimentalContracts::class)
inline fun IrFactory.buildFunctionWithLateBinding(init: IrFunctionWithLateBindingBuilder.() -> Unit): IrFunctionWithLateBinding {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return build(IrFunctionWithLateBindingBuilder().apply(init))
}
