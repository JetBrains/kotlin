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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class InComparableRangeLiteralGenerator(
        codegen: ExpressionCodegen,
        operatorReference: KtSimpleNameExpression,
        rangeCall: ResolvedCall<out CallableDescriptor>
) : AbstractInRangeWithKnownBoundsExpressionGenerator(
        codegen, operatorReference, true,
        Type.getType("Ljava/lang/Comparable;")
) {
    private val from: ReceiverValue = rangeCall.extensionReceiver!!
    private val to: KtExpression = ExpressionCodegen.getSingleArgumentExpression(rangeCall)!!

    override fun genLowBound(): StackValue = codegen.generateReceiverValue(from, false)

    override fun genHighBound(): StackValue = codegen.gen(to)

    override val comparisonGenerator: ComparisonGenerator = ComparableComparisonGenerator

    private object ComparableComparisonGenerator : ComparisonGenerator {
        override fun jumpIfGreaterOrEqual(v: InstructionAdapter, label: Label) {
            invokeCompare(v)
            v.ifge(label)
        }

        override fun jumpIfLessOrEqual(v: InstructionAdapter, label: Label) {
            invokeCompare(v)
            v.ifle(label)
        }

        override fun jumpIfGreater(v: InstructionAdapter, label: Label) {
            invokeCompare(v)
            v.ifgt(label)
        }

        override fun jumpIfLess(v: InstructionAdapter, label: Label) {
            invokeCompare(v)
            v.iflt(label)
        }

        private fun invokeCompare(v: InstructionAdapter) {
            v.invokeinterface("java/lang/Comparable", "compareTo", "(Ljava/lang/Object;)I")
        }
    }
}