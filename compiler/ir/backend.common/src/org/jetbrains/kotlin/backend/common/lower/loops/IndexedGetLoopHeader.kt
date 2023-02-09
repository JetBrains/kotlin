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
import org.jetbrains.kotlin.ir.util.implicitCastIfNeededTo

class IndexedGetLoopHeader(
    headerInfo: IndexedGetHeaderInfo,
    builder: DeclarationIrBuilder,
    context: CommonBackendContext
) : NumericForLoopHeader<IndexedGetHeaderInfo>(headerInfo, builder, context) {

    private val preferJavaLikeCounterLoop = context.preferJavaLikeCounterLoop
    private val javaLikeCounterLoopBuilder = JavaLikeCounterLoopBuilder(context)

    override val loopInitStatements =
        listOfNotNull(headerInfo.objectVariable, inductionVariable, lastVariableIfCanCacheLast, stepVariable)

    override fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder,
        backendContext: CommonBackendContext,
    ): List<IrStatement> =
        with(builder) {
            // loopVariable = objectVariable[inductionVariable]
            val indexedGetFun = with(headerInfo.expressionHandler) { headerInfo.objectVariable.type.getFunction }
            // Making sure that expression type has type of the variable when it exists.
            // Return type of get function can be a type parameter (for example Array<T>::get) which is not a subtype of loopVariable type.
            val get = irCall(indexedGetFun.symbol, indexedGetFun.returnType).apply {
                dispatchReceiver = irGet(headerInfo.objectVariable)
                putValueArgument(0, irGet(inductionVariable))
            }.implicitCastIfNeededTo(loopVariable?.type ?: indexedGetFun.returnType)
            // The call could be wrapped in an IMPLICIT_NOTNULL type-cast (see comment in ForLoopsLowering.gatherLoopVariableInfo()).
            // Find and replace the call to preserve any type-casts.
            loopVariable?.initializer = loopVariable?.initializer?.transform(InitializerCallReplacer(get), null)
            // Even if there is no loop variable, we always want to call `get()` as it may have side effects.
            // The un-lowered loop always calls `get()` on each iteration.
            listOf(loopVariable ?: get) + incrementInductionVariable(this)
        }

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement = with(builder) {
        val newLoopCondition = buildLoopCondition(this@with)
        if (preferJavaLikeCounterLoop) {
            javaLikeCounterLoopBuilder.buildJavaLikeDoWhileCounterLoop(oldLoop, newLoopCondition, newBody, loopOrigin = null)
        } else {
            // Loop is lowered into something like:
            //
            //   var inductionVar = 0
            //   var last = objectVariable.size
            //   while (inductionVar < last) {
            //       val loopVar = objectVariable.get(inductionVar)
            //       inductionVar++
            //       // Loop body
            //   }
            val newLoop = IrWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
                label = oldLoop.label
                condition = newLoopCondition
                body = newBody
            }
            LoopReplacement(newLoop, newLoop)
        }
    }
}