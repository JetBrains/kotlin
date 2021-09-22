/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.backend.common.lower.matchers.SimpleCalleeMatcher
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.isUnsignedArray
import org.jetbrains.kotlin.name.FqName

/** Builds a [HeaderInfo] for progressions built using the `indices` extension property. */
internal abstract class IndicesHandler(protected val context: CommonBackendContext) :
    ProgressionHandler {

    private val preferJavaLikeCounterLoop = context.preferJavaLikeCounterLoop

    override fun build(expression: IrCall, data: ProgressionType, scopeOwner: IrSymbol): HeaderInfo? =
        with(context.createIrBuilder(scopeOwner, expression.startOffset, expression.endOffset)) {
            val last: IrExpression
            val lastInclusive: IrExpression?
            val isLastInclusive: Boolean

            if (preferJavaLikeCounterLoop) {
                // Convert range with inclusive upper bound to exclusive upper bound if possible.
                // This affects loop code performance on JVM.
                last = irCall(expression.symbol.owner.extensionReceiverParameter!!.type.sizePropertyGetter)
                    .apply { dispatchReceiver = expression.extensionReceiver!! }
                lastInclusive = last.decrement()
                isLastInclusive = false
            } else {
                last = irCall(expression.symbol.owner.extensionReceiverParameter!!.type.sizePropertyGetter)
                    .apply { dispatchReceiver = expression.extensionReceiver!! }
                    .decrement()
                lastInclusive = null
                isLastInclusive = true
            }

            ProgressionHeaderInfo(
                data,
                first = irInt(0),
                last = last,
                step = irInt(1),
                isLastInclusive = isLastInclusive,
                canOverflow = false,
                direction = ProgressionDirection.INCREASING,
                originalLastInclusive = lastInclusive
            )
        }

    abstract val IrType.sizePropertyGetter: IrSimpleFunction
}

internal class CollectionIndicesHandler(context: CommonBackendContext) : IndicesHandler(context) {

    override val matcher = SimpleCalleeMatcher {
        extensionReceiver { it?.type?.isCollection() == true }
        fqName { it == FqName("kotlin.collections.<get-indices>") }
        parameterCount { it == 0 }
    }

    override val IrType.sizePropertyGetter: IrSimpleFunction
        get() = context.ir.symbols.collection.getPropertyGetter("size")!!.owner
}

internal class ArrayIndicesHandler(context: CommonBackendContext) : IndicesHandler(context) {
    private val supportsUnsignedArrays = context.optimizeLoopsOverUnsignedArrays

    override val matcher = SimpleCalleeMatcher {
        extensionReceiver {
            it != null && it.type.run {
                isArray() || isPrimitiveArray() || (supportsUnsignedArrays && isUnsignedArray())
            }
        }
        fqName { it == FqName("kotlin.collections.<get-indices>") }
        parameterCount { it == 0 }
    }

    override val IrType.sizePropertyGetter: IrSimpleFunction
        get() = getClass()!!.getPropertyGetter("size")!!.owner
}

internal class CharSequenceIndicesHandler(context: CommonBackendContext) : IndicesHandler(context) {

    override val matcher = SimpleCalleeMatcher {
        extensionReceiver { it != null && it.type.run { isCharSequence() } }
        fqName { it == FqName("kotlin.text.<get-indices>") }
        parameterCount { it == 0 }
    }

    override val IrType.sizePropertyGetter: IrSimpleFunction
        get() = context.ir.symbols.charSequence.getPropertyGetter("length")!!.owner
}