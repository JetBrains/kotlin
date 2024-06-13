/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.irCastIfNeeded

class ProgressionLoopHeader(
    headerInfo: ProgressionHeaderInfo,
    builder: DeclarationIrBuilder,
    context: CommonBackendContext
) : NumericForLoopHeader<ProgressionHeaderInfo>(headerInfo, builder, context) {

    private val preferJavaLikeCounterLoop = context.preferJavaLikeCounterLoop
    private val javaLikeCounterLoopBuilder = JavaLikeCounterLoopBuilder(context)

    // For this loop:
    //
    //   for (i in first()..last() step step())
    //
    // ...the functions may have side effects, so we need to call them in the following order: first() (inductionVariable), last(), step().
    // Additional variables come first as they may be needed to the subsequent variables.
    //
    // In the case of a reversed range, the `inductionVariable` and `last` variables are swapped, therefore the declaration order must be
    // swapped to preserve the correct evaluation order.
    override val loopInitStatements = headerInfo.additionalStatements + (
            if (headerInfo.isReversed)
                listOfNotNull(lastVariableIfCanCacheLast, inductionVariable)
            else
                listOfNotNull(inductionVariable, lastVariableIfCanCacheLast)
            ) +
            listOfNotNull(stepVariable)

    private var loopVariable: IrVariable? = null

    override fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder,
        backendContext: CommonBackendContext,
    ): List<IrStatement> =
        with(builder) {
            // loopVariable is used in the loop condition if it can overflow. If no loopVariable was provided, create one.
            this@ProgressionLoopHeader.loopVariable = if (headerInfo.canOverflow && loopVariable == null) {
                scope.createTmpVariable(
                    irGet(inductionVariable),
                    nameHint = "loopVariable",
                    isMutable = true
                )
            } else {
                loopVariable?.initializer = irGet(inductionVariable).let {
                    headerInfo.progressionType.run {
                        if (this is UnsignedProgressionType) {
                            // The induction variable is signed for unsigned progressions but the loop variable should be unsigned.
                            it.asUnsigned()
                        } else it
                    }
                }
                loopVariable
            }

            // loopVariable = inductionVariable
            // inductionVariable = inductionVariable + step
            listOfNotNull(this@ProgressionLoopHeader.loopVariable, incrementInductionVariable(this))
        }

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?) =
        with(builder) {
            if (headerInfo.canOverflow ||
                preferJavaLikeCounterLoop && headerInfo.progressionType is UnsignedProgressionType && headerInfo.isLastInclusive
            ) {
                // If the induction variable CAN overflow, we cannot use it in the loop condition.
                // Loop is lowered into something like:
                //
                //   if (inductionVar <= last) {
                //     // Loop is not empty
                //     do {
                //       val loopVar = inductionVar
                //       inductionVar += step
                //       // Loop body
                //     } while (loopVar != last)
                //   }
                //
                // This loop form is also preferable for loops over unsigned progressions on JVM,
                // because HotSpot doesn't recognize unsigned integer comparison as a counter loop condition.
                // Unsigned integer equality is fine, though.
                // See KT-49444 for performance comparison example.
                val newLoopOrigin = if (preferJavaLikeCounterLoop)
                    this@ProgressionLoopHeader.context.doWhileCounterLoopOrigin
                else
                    oldLoop.origin
                val newLoop = IrDoWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, newLoopOrigin).apply {
                    // Inliner might erase type of `loopVariable` to `Any`, so loopVariableExpression needs a cast to the known type,
                    // which is usually `headerInfo.progressionType.elementClass`
                    val loopVariableExpression = headerInfo.progressionType.run {
                        if (this is UnsignedProgressionType) {
                            // The loop variable is signed but bounds are signed for unsigned progressions.
                            // Also, cannot use unreliable `elementClass` here: it depends on `allowUnsignedBounds`
                            irCastIfNeeded(irGet(loopVariable!!), unsignedType).asSigned()
                        } else {
                            irCastIfNeeded(irGet(loopVariable!!), elementClass.defaultType)
                        }
                    }
                    label = oldLoop.label
                    condition = irNotEquals(loopVariableExpression, lastExpression)
                    body = newBody
                }

                if (preferJavaLikeCounterLoop) {
                    javaLikeCounterLoopBuilder.moveInductionVariableUpdateToLoopCondition(newLoop)
                }

                val loopCondition = buildLoopCondition(this@with)
                LoopReplacement(newLoop, irIfThen(loopCondition, newLoop))
            } else if (preferJavaLikeCounterLoop && !headerInfo.isLastInclusive) {
                // It is critically important for loop code performance on JVM to "look like" a simple counter loop in Java when possible
                // (`for (int i = first; i < lastExclusive; ++i) { ... }`).
                // Otherwise loop-related optimizations will not kick in, resulting in significant performance degradation.
                //
                // Use a do-while loop:
                //   do {
                //       if ( !( inductionVariable < last ) ) break
                //       val loopVariable = inductionVariable
                //       <body>
                //   } while ( { inductionVariable += step; true } )
                // This loop form is equivalent to the Java counter loop shown above.

                val newLoopCondition = buildLoopCondition(this@with)

                javaLikeCounterLoopBuilder.buildJavaLikeDoWhileCounterLoop(
                    oldLoop, newLoopCondition, newBody,
                    this@ProgressionLoopHeader.context.doWhileCounterLoopOrigin
                )
            } else {
                // Use an if-guarded do-while loop (note the difference in loop condition):
                //
                //   if (inductionVar <= last) {
                //     do {
                //       val loopVar = inductionVar
                //       inductionVar += step
                //       // Loop body
                //     } while (inductionVar <= last)
                //   }
                //
                val newLoop = IrDoWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
                    label = oldLoop.label
                    condition = buildLoopCondition(this@with)
                    body = newBody
                }
                val loopCondition = buildLoopCondition(this@with)
                LoopReplacement(newLoop, irIfThen(loopCondition, newLoop))
            }
        }

}

