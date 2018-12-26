/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.range.forLoop

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.range.SimpleBoundedValue
import org.jetbrains.kotlin.codegen.range.comparison.ComparisonGenerator
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.org.objectweb.asm.Label

class ForInSimpleProgressionLoopGenerator(
    codegen: ExpressionCodegen,
    forExpression: KtForExpression,
    private val startValue: StackValue,
    private val isStartInclusive: Boolean,
    private val endValue: StackValue,
    private val isEndInclusive: Boolean,
    private val inverseBoundsEvaluationOrder: Boolean,
    comparisonGenerator: ComparisonGenerator,
    step: Int
) : AbstractForInRangeLoopGenerator(codegen, forExpression, step, comparisonGenerator) {

    constructor(
        codegen: ExpressionCodegen,
        forExpression: KtForExpression,
        boundedValue: SimpleBoundedValue,
        inverseBoundsEvaluationOrder: Boolean,
        comparisonGenerator: ComparisonGenerator,
        step: Int
    ) : this(
        codegen, forExpression,
        startValue = if (step == 1) boundedValue.lowBound else boundedValue.highBound,
        isStartInclusive = if (step == 1) boundedValue.isLowInclusive else boundedValue.isHighInclusive,
        endValue = if (step == 1) boundedValue.highBound else boundedValue.lowBound,
        isEndInclusive = if (step == 1) boundedValue.isHighInclusive else boundedValue.isLowInclusive,
        inverseBoundsEvaluationOrder = inverseBoundsEvaluationOrder,
        comparisonGenerator = comparisonGenerator,
        step = step
    )

    companion object {
        fun fromBoundedValueWithStep1(
            codegen: ExpressionCodegen,
            forExpression: KtForExpression,
            boundedValue: SimpleBoundedValue,
            comparisonGenerator: ComparisonGenerator,
            inverseBoundsEvaluationOrder: Boolean = false
        ) =
            ForInSimpleProgressionLoopGenerator(codegen, forExpression, boundedValue, inverseBoundsEvaluationOrder, comparisonGenerator, 1)

        fun fromBoundedValueWithStepMinus1(
            codegen: ExpressionCodegen,
            forExpression: KtForExpression,
            boundedValue: SimpleBoundedValue,
            comparisonGenerator: ComparisonGenerator,
            inverseBoundsEvaluationOrder: Boolean = false
        ) =
            ForInSimpleProgressionLoopGenerator(codegen, forExpression, boundedValue, inverseBoundsEvaluationOrder, comparisonGenerator, -1)
    }

    override fun storeRangeStartAndEnd() {
        if (inverseBoundsEvaluationOrder) {
            StackValue.local(endVar, asmElementType).store(endValue, v)
            loopParameter().store(startValue, v)
        } else {
            loopParameter().store(startValue, v)
            StackValue.local(endVar, asmElementType).store(endValue, v)
        }

        // Skip 1st element if start is not inclusive.
        if (!isStartInclusive) incrementLoopVariable()
    }

    override fun checkEmptyLoop(loopExit: Label) {
        // No check required if end is non-inclusive: loop is generated with pre-condition.
        if (isEndInclusive) {
            super.checkEmptyLoop(loopExit)
        }
    }

    override fun checkPreCondition(loopExit: Label) {
        // Generate pre-condition loop if end is not inclusive.
        if (!isEndInclusive) {
            loopParameter().put(asmElementType, elementType, v)
            v.load(endVar, asmElementType)
            if (step > 0)
                comparisonGenerator.jumpIfGreaterOrEqual(v, loopExit)
            else
                comparisonGenerator.jumpIfLessOrEqual(v, loopExit)
        }
    }

    override fun checkPostConditionAndIncrement(loopExit: Label) {
        // Generate post-condition loop if end is inclusive.
        // Otherwise, just increment the loop variable.
        if (isEndInclusive) {
            super.checkPostConditionAndIncrement(loopExit)
        } else {
            incrementLoopVariable()
        }
    }
}