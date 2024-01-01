/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See compiler/ir/ir.tree/tree-generator/ReadMe.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

/**
 * A leaf IR tree element.
 *
 * Generated from: [org.jetbrains.kotlin.ir.generator.IrTree.valueParameter]
 */
abstract class IrValueParameter : IrDeclarationBase(), IrValueDeclaration {
    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: ParameterDescriptor

    abstract override val symbol: IrValueParameterSymbol

    abstract var index: Int

    abstract var varargElementType: IrType?

    abstract var isCrossinline: Boolean

    abstract var isNoinline: Boolean

    /**
     * If `true`, the value parameter does not participate in [IdSignature] computation.
     *
     * This is a workaround that is needed for better support of compiler plugins.
     * Suppose you have the following code and some IR plugin that adds a value parameter to functions
     * marked with the `@PluginMarker` annotation.
     * ```kotlin
     * @PluginMarker
     * fun foo(defined: Int) { /* ... */ }
     * ```
     *
     * Suppose that after applying the plugin the function is changed to:
     * ```kotlin
     * @PluginMarker
     * fun foo(defined: Int, $extra: String) { /* ... */ }
     * ```
     *
     * If a compiler plugin adds parameters to an [IrFunction],
     * the representations of the function in the frontend and in the backend may diverge, potentially causing signature mismatch and
     * linkage errors (see [KT-40980](https://youtrack.jetbrains.com/issue/KT-40980)).
     * We wouldn't want IR plugins to affect the frontend representation, since in an IDE you'd want to be able to see those
     * declarations in their original form (without the `$extra` parameter).
     *
     * To fix this problem, [isHidden] was introduced.
     *
     * TODO: consider dropping [isHidden] if it isn't used by any known plugin.
     */
    abstract var isHidden: Boolean

    abstract var defaultValue: IrExpressionBody?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitValueParameter(this, data)

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrValueParameter =
        accept(transformer, data) as IrValueParameter

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        defaultValue?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        defaultValue = defaultValue?.transform(transformer, data)
    }
}
