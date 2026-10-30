/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfo
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfoHandler
import org.jetbrains.kotlin.backend.common.lower.loops.IndexedGetHeaderInfo
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.OperatorNameConventions

/** Builds a [HeaderInfo] for iteration over iterables using the `get / []` operator and an index. */
abstract class IndexedGetIterationHandler(
    protected val context: CommonBackendContext,
    private val canCacheLast: Boolean
) : HeaderInfoHandler<IrExpression, Nothing?> {
    override fun build(expression: IrExpression, data: Nothing?, scopeOwner: IrSymbol): HeaderInfo? = TODO()

    abstract val IrType.sizePropertyGetter: IrSimpleFunction

    abstract val IrType.getFunction: IrSimpleFunction
}

/** Builds a [HeaderInfo] for arrays. */
internal class ArrayIterationHandler(context: CommonBackendContext) : IndexedGetIterationHandler(context, canCacheLast = true) {
    private val supportsUnsignedArrays = context.optimizeLoopsOverUnsignedArrays

    override fun matchIterable(expression: IrExpression): Boolean { TODO() }

    override val IrType.sizePropertyGetter: IrSimpleFunction
        get() = context.symbols.arraySizePropertyGetter(this)

    private fun IrType.isArrayType() =
        isArray() || isPrimitiveArray() || (supportsUnsignedArrays && isUnsignedArray())

    override val IrType.getFunction: IrSimpleFunction
        get() = context.symbols.arrayElementGetter(this, context.irBuiltIns.intType)
}

/**
 * Builds a [HeaderInfo] for iteration over characters in a [CharSequence].
 *
 * Note: The value for "last" can NOT be cached (i.e., stored in a variable) because the size/length can change within the loop. This means
 * that "last" is re-evaluated with each iteration of the loop.
 */
internal open class CharSequenceIterationHandler(
    context: CommonBackendContext,
    canCacheLast: Boolean = false
) : IndexedGetIterationHandler(context, canCacheLast) {
    override fun matchIterable(expression: IrExpression): Boolean = TODO()

    // We only want to handle the known extension function for CharSequence in the standard library (top level `kotlin.text.iterator`).
    // The behavior of this iterator is well-defined and can be lowered. CharSequences can have their own iterators, either as a member or
    // extension function, and the behavior of those custom iterators is unknown.
    override fun matchIteratorCall(call: IrCall): Boolean { TODO() }

    override val IrType.sizePropertyGetter: IrSimpleFunction
        get() = context.irBuiltIns.charSequenceClass.getPropertyGetter("length")!!.owner

    override val IrType.getFunction: IrSimpleFunction
        get() = context.irBuiltIns.charSequenceClass.getSimpleFunction(OperatorNameConventions.GET.asString())!!.owner
}

/**
 * Builds a [HeaderInfo] for iteration over characters in a [String].
 *
 * Note: The value for "last" CAN be cached for Strings as they are immutable and the size/length cannot change.
 */
internal class StringIterationHandler(context: CommonBackendContext) : CharSequenceIterationHandler(context, canCacheLast = true) {
    override fun matchIterable(expression: IrExpression): Boolean = TODO()

    override val IrType.sizePropertyGetter: IrSimpleFunction
        get() = context.irBuiltIns.stringClass.getPropertyGetter("length")!!.owner

    override val IrType.getFunction: IrSimpleFunction
        get() = context.irBuiltIns.stringClass.getSimpleFunction(OperatorNameConventions.GET.asString())!!.owner
}
