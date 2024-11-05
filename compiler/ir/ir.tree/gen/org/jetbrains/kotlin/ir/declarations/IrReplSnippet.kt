/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrReplSnippetSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.util.transformInPlace
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * Represents a REPL snippet entity that corresponds to the analogous FIR entity.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.replSnippet]
 */
abstract class IrReplSnippet : IrDeclarationBase(), IrDeclarationWithName, IrDeclarationParent, IrMetadataSourceOwner {
    abstract override val symbol: IrReplSnippetSymbol

    /**
     * Stores implicit receiver parameters configured for the snippet.
     */
    abstract var receiverParameters: List<IrValueParameter>

    abstract val variablesFromOtherSnippets: MutableList<IrVariable>

    abstract val declarationsFromOtherSnippets: MutableList<IrDeclaration>

    /**
     * Contains link to the static state object for this compilation session.
     */
    abstract var stateObject: IrClassSymbol?

    abstract var body: IrBody

    abstract var returnType: IrType?

    /**
     * Contains link to the IrClass symbol to which this snippet should be lowered on the appropriate stage.
     */
    abstract var targetClass: IrClassSymbol?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitReplSnippet(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        receiverParameters.forEach { it.accept(visitor, data) }
        variablesFromOtherSnippets.forEach { it.accept(visitor, data) }
        declarationsFromOtherSnippets.forEach { it.accept(visitor, data) }
        body.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        receiverParameters = receiverParameters.transformIfNeeded(transformer, data)
        variablesFromOtherSnippets.transformInPlace(transformer, data)
        declarationsFromOtherSnippets.transformInPlace(transformer, data)
        body = body.transform(transformer, data)
    }
}
