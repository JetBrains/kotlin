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

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.range.forLoop.ForLoopGenerator
import org.jetbrains.kotlin.codegen.range.inExpression.InExpressionGenerator
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

interface RangeValue {
    fun createForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression): ForLoopGenerator

    fun createInExpressionGenerator(codegen: ExpressionCodegen, operatorReference: KtSimpleNameExpression): InExpressionGenerator
}


interface ReversableRangeValue : RangeValue {
    fun createForInReversedLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression): ForLoopGenerator
}


interface BoundedValue {
    val instanceType: Type

    fun putInstance(v: InstructionAdapter, type: Type)

    // It is necessary to maintain the proper evaluation order as of Kotlin 1.0 and 1.1
    // to evaluate range bounds left to right and put them on stack as 'high; low'.
    fun putHighLow(v: InstructionAdapter, type: Type)

    val isLowInclusive: Boolean
    val isHighInclusive: Boolean
}

fun BoundedValue.asStackValue(): StackValue =
        object : StackValue(instanceType) {
            override fun putSelector(type: Type, v: InstructionAdapter) {
                putInstance(v, type)
            }
        }
