/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops.handlers

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfoBuilder
import org.jetbrains.kotlin.backend.common.lower.loops.HeaderInfoHandler
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName

/** Builds a [HeaderInfo] for calls to reverse an iterable. */
internal class ReversedHandler(context: CommonBackendContext, private val visitor: HeaderInfoBuilder) :
    HeaderInfoHandler<IrCall, Nothing?> {
    private val progressionClassesTypes = context.ir.symbols.progressionClasses.map { it.defaultType }.toSet()

    override fun matchIterable(expression: IrCall): Boolean {
        // TODO: Handle reversed String, Progression.withIndex(), etc.
        val callee = expression.symbol.owner
        return callee.valueParameters.isEmpty() &&
                callee.extensionReceiverParameter?.type in progressionClassesTypes &&
                callee.kotlinFqName == FqName("kotlin.ranges.reversed")
    }

    // Reverse the HeaderInfo from the underlying progression or array (if any).
    override fun build(expression: IrCall, data: Nothing?, scopeOwner: IrSymbol) =
        expression.extensionReceiver!!.accept(visitor, null)?.asReversed()
}
