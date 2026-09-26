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
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrReplSnippet
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrReplSnippetImpl
import org.jetbrains.kotlin.ir.declarations.impl.REPL_SNIPPET_ORIGIN
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrReplSnippetSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrReplSnippetSymbolImpl
import org.jetbrains.kotlin.name.Name

/**
 * Collects the properties of a [IrReplSnippet] and builds one.
 *
 * A property with no sensible default is declared `lateinit`, so building without it throws rather than
 * inventing a value. There is no way to express "required" for a property the caller assigns inside a
 * lambda; a constructor parameter could, at the cost of the property being settable only once.
 *
 * Built by [IrFactory.build], which is where the factory comes from.
 */
@IrBuilderDsl
class IrReplSnippetBuilder {
    var startOffset: Int = UNDEFINED_OFFSET
    var endOffset: Int = UNDEFINED_OFFSET
    lateinit var name: Name
    var symbol: IrReplSnippetSymbol = IrReplSnippetSymbolImpl()
    var origin: IrDeclarationOrigin = REPL_SNIPPET_ORIGIN
    var metadata: MetadataSource? = null
    var stateObject: IrClassSymbol? = null
    var targetClass: IrClassSymbol? = null

    /**
     * Takes the source range of [from], the way `IrElement.startOffset` and `endOffset` are usually copied.
     */
    fun setSourceRange(from: IrElement) {
        startOffset = from.startOffset
        endOffset = from.endOffset
    }

    /**
     * Copies the properties of [from] that identify a *kind* of IrReplSnippet, not a particular one,
     * including its source range.
     *
     * Its symbol, parent and children are left alone: sharing a symbol between two declarations would bind both
     * to one, and sharing a body or a receiver would splice one tree into another. Properties the element
     * declares `lateinit` are skipped too, since reading one that was never set throws.
     */
    fun updateFrom(from: IrReplSnippet) {
        startOffset = from.startOffset
        endOffset = from.endOffset
        name = from.name
        origin = from.origin
    }
}

/**
 * Builds the collected [IrReplSnippet].
 *
 * `declarationCreated` is not decoration: `IrFactoryImplForJsIC` and `IrFactoryImplForWasmIC` override it to
 * attach an `IdSignature` to every declaration they create. Skipping it would leave declarations unsigned, and
 * incremental compilation would cache the wrong thing without failing.
 */
@PublishedApi
internal fun IrFactory.build(builder: IrReplSnippetBuilder): IrReplSnippet {
    with(builder) {
        val result = IrReplSnippetImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            factory = this@build,
            name = name,
            symbol = symbol,
        ).declarationCreated()
        result.origin = origin
        result.metadata = metadata
        result.stateObject = stateObject
        result.targetClass = targetClass
        return result
    }
}

@OptIn(ExperimentalContracts::class)
inline fun IrFactory.buildReplSnippet(init: IrReplSnippetBuilder.() -> Unit): IrReplSnippet {
    contract {
        callsInPlace(init, InvocationKind.EXACTLY_ONCE)
    }
    return build(IrReplSnippetBuilder().apply(init))
}
