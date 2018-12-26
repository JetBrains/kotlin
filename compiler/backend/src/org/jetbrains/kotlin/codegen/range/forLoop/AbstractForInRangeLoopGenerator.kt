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

import org.jetbrains.kotlin.codegen.AsmUtil.genIncrement
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.range.comparison.ComparisonGenerator
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

abstract class AbstractForInRangeLoopGenerator(
    codegen: ExpressionCodegen,
    forExpression: KtForExpression,
    protected val step: Int,
    protected val comparisonGenerator: ComparisonGenerator
) : AbstractForInProgressionOrRangeLoopGenerator(codegen, forExpression) {

    override fun beforeLoop() {
        super.beforeLoop()

        storeRangeStartAndEnd()
    }

    protected abstract fun storeRangeStartAndEnd()

    override fun checkEmptyLoop(loopExit: Label) {
        loopParameter().put(asmElementType, elementType, v)
        v.load(endVar, asmElementType)

        if (step > 0) {
            comparisonGenerator.jumpIfGreater(v, loopExit)
        } else {
            comparisonGenerator.jumpIfLess(v, loopExit)
        }
    }

    override fun assignToLoopParameter() {}

    override fun checkPostConditionAndIncrement(loopExit: Label) {
        checkPostCondition(loopExit)

        incrementLoopVariable()
    }

    protected fun incrementLoopVariable() {
        if (loopParameterType === Type.INT_TYPE) {
            v.iinc(loopParameterVar, step)
        } else {
            val loopParameter = loopParameter()
            loopParameter.put(asmElementType, elementType, v)
            genIncrement(asmElementType, step, v)
            loopParameter.store(StackValue.onStack(asmElementType, elementType), v)
        }
    }

    init {
        assert(step == 1 || step == -1) { "'step' should be either 1 or -1: " + step }
    }
}
