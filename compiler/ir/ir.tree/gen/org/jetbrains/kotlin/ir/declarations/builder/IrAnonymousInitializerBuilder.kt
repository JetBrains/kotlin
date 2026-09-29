/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

@file:Suppress("DuplicatedCode", "unused")

package org.jetbrains.kotlin.ir.declarations.builder

import kotlin.contracts.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.impl.IrAnonymousInitializerImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.symbols.IrAnonymousInitializerSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl

/**
 * Collects the properties of a [IrAnonymousInitializer] and builds one.
 *
 * A property with no sensible default is declared `lateinit`, so building without it throws rather than
 * inventing a value. There is no way to express "required" for a property the caller assigns inside a
 * lambda; a constructor parameter could, at the cost of the property being settable only once.
 *
 * Built by [IrFactory.build], which is where the factory comes from.
 */
@IrBuilderDsl
class IrAnonymousInitializerBuilder {
    var startOffset: Int = UNDEFINED_OFFSET
    var endOffset: Int = UNDEFINED_OFFSET
    var origin: IrDeclarationOrigin = IrDeclarationOrigin.DEFINED
    var symbol: IrAnonymousInitializerSymbol = IrAnonymousInitializerSymbolImpl()
    var isStatic: Boolean = false
    var body: IrBlockBody? = null

    /**
     * Takes the source range of [from], the way `IrElement.startOffset` and `endOffset` are usually copied.
     */
    fun setSourceRange(from: IrElement) {
        startOffset = from.startOffset
        endOffset = from.endOffset
    }

    /**
     * Copies the properties of [from] that identify a *kind* of IrAnonymousInitializer, not a particular one,
     * including its source range.
     *
     * Its symbol, parent and children are left alone: sharing a symbol between two declarations would bind both
     * to one, and sharing a body or a receiver would splice one tree into another. Properties the element
     * declares `lateinit` are skipped too, since reading one that was never set throws.
     */
    fun updateFrom(from: IrAnonymousInitializer) {
        startOffset = from.startOffset
        endOffset = from.endOffset
        origin = from.origin
        isStatic = from.isStatic
    }
}

/**
 * Builds the collected [IrAnonymousInitializer].
 *
 * `declarationCreated` is not decoration: `IrFactoryImplForJsIC` and `IrFactoryImplForWasmIC` override it to
 * attach an `IdSignature` to every declaration they create. Skipping it would leave declarations unsigned, and
 * incremental compilation would cache the wrong thing without failing.
 */
@OptIn(IrImplementationDetail::class)
@PublishedApi
internal fun IrFactory.build(builder: IrAnonymousInitializerBuilder): IrAnonymousInitializer {
    with(builder) {
        val result = IrAnonymousInitializerImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            origin = origin,
            factory = this@build,
            symbol = symbol,
            isStatic = isStatic,
        ).declarationCreated()
        body?.let { result.body = it }
        return result
    }
}

@OptIn(ExperimentalContracts::class)
inline fun IrFactory.buildAnonymousInitializer(init: IrAnonymousInitializerBuilder.() -> Unit = {}): IrAnonymousInitializer {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return build(IrAnonymousInitializerBuilder().apply(init))
}
