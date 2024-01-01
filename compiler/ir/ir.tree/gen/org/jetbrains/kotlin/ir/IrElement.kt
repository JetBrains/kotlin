/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * The root interface of the IR tree. Each IR node implements this interface.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.rootElement]
 */
interface IrElement {
    /**
     * The start offset of the syntax node from which this IR node was generated,
     * in number of characters from the start of the source file. If there is no source information for this IR node,
     * the [UNDEFINED_OFFSET] constant is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    val startOffset: Int

    /**
     * The end offset of the syntax node from which this IR node was generated,
     * in number of characters from the start of the source file. If there is no source information for this IR node,
     * the [UNDEFINED_OFFSET] constant is used. In order to get the line number and the column number from this offset,
     * [IrFileEntry.getLineNumber] and [IrFileEntry.getColumnNumber] can be used.
     *
     * @see IrFileEntry.getSourceRangeInfo
     */
    val endOffset: Int

    /**
     * Runs the provided [visitor] on the IR subtree with the root at this node.
     *
     * @param visitor The visitor to accept.
     * @param data An arbitrary context to pass to each invocation of [visitor]'s methods.
     * @return The value returned by the topmost `visit*` invocation.
     */
    fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R

    /**
     * Runs the provided [transformer] on the IR subtree with the root at this node.
     *
     * @param transformer The transformer to use.
     * @param data An arbitrary context to pass to each invocation of [transformer]'s methods.
     * @return The transformed node.
     */
    fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrElement

    /**
     * Runs the provided [visitor] on subtrees with roots in this node's children.
     *
     * Basically, calls `accept(visitor, data)` on each child of this node.
     *
     * Does **not** run [visitor] on this node itself.
     *
     * @param visitor The visitor for children to accept.
     * @param data An arbitrary context to pass to each invocation of [visitor]'s methods.
     */
    fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D)

    /**
     * Recursively transforms this node's children *in place* using [transformer].
     *
     * Basically, executes `this.child = this.child.transform(transformer, data)` for each child of this node.
     *
     * Does **not** run [transformer] on this node itself.
     *
     * @param transformer The transformer to use for transforming the children.
     * @param data An arbitrary context to pass to each invocation of [transformer]'s methods.
     */
    fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D)
}
