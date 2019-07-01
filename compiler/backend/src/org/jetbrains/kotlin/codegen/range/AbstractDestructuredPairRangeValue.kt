/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.range.forLoop.ForLoopGenerator
import org.jetbrains.kotlin.codegen.range.forLoop.IteratorForLoopGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.CallBasedInExpressionGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InExpressionGenerator
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

abstract class AbstractDestructuredPairRangeValue(protected val rangeCall: ResolvedCall<out CallableDescriptor>) :
    RangeValue {

    override fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression): ForLoopGenerator {
        val loopParameter = forExpression.destructuringDeclaration
        return if (loopParameter != null)
            createDestructuredPairForLoopGenerator(codegen, forExpression, loopParameter, rangeCall)
        else
            IteratorForLoopGenerator(codegen, forExpression)
    }

    protected abstract fun createDestructuredPairForLoopGenerator(
        codegen: ExpressionCodegen,
        forExpression: KtForExpression,
        loopParameter: KtDestructuringDeclaration,
        rangeCall: ResolvedCall<out CallableDescriptor>
    ): ForLoopGenerator

    override fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator =
        CallBasedInExpressionGenerator(codegen, operatorReference)
}


