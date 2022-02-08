/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrWhileLoopImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.coerceToUnitIfNeeded
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class IterableLoopHeader(
    private val headerInfo: IterableHeaderInfo
) : ForLoopHeader {
    override val loopInitStatements = listOf(headerInfo.iteratorVariable)

    override val consumesLoopVariableComponents = false

    override fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder,
        backendContext: CommonBackendContext,
    ): List<IrStatement> =
        with(builder) {
            // loopVariable = iteratorVar.next()
            val iteratorClass = headerInfo.iteratorVariable.type.getClass()!!
            val next =
                irCall(iteratorClass.functions.first {
                    it.name == OperatorNameConventions.NEXT && it.valueParameters.isEmpty()
                }.symbol).apply {
                    dispatchReceiver = irGet(headerInfo.iteratorVariable)
                }
            // The call could be wrapped in an IMPLICIT_NOTNULL type-cast (see comment in ForLoopsLowering.gatherLoopVariableInfo()).
            // Find and replace the call to preserve any type-casts.
            loopVariable?.initializer = loopVariable?.initializer?.transform(InitializerCallReplacer(next), null)
            // Even if there is no loop variable, we always want to call `next()` for iterables and sequences.
            listOf(loopVariable ?: next.coerceToUnitIfNeeded(next.type, context.irBuiltIns, backendContext.typeSystem))
        }

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement = with(builder) {
        // Loop is lowered into something like:
        //
        //   var iteratorVar = someIterable.iterator()
        //   while (iteratorVar.hasNext()) {
        //       val loopVar = iteratorVar.next()
        //       // Loop body
        //   }
        val iteratorClass = headerInfo.iteratorVariable.type.getClass()!!
        val hasNext =
            irCall(iteratorClass.functions.first { it.name == OperatorNameConventions.HAS_NEXT && it.valueParameters.isEmpty() }).apply {
                dispatchReceiver = irGet(headerInfo.iteratorVariable)
            }
        val newLoop = IrWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
            label = oldLoop.label
            condition = hasNext
            body = newBody
        }
        LoopReplacement(newLoop, newLoop)
    }
}