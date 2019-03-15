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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.BindingContext.*
import org.jetbrains.kotlin.resolve.BindingContextUtils.getNotNull
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

class IteratorForLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) :
    AbstractForLoopGenerator(codegen, forExpression) {
    private var iteratorVarIndex: Int = 0
    private val iteratorCall: ResolvedCall<FunctionDescriptor>
    private val nextCall: ResolvedCall<FunctionDescriptor>
    private val iteratorType: KotlinType
    private val asmTypeForIterator: Type

    init {
        val loopRange = forExpression.loopRange!!
        this.iteratorCall = getNotNull(
            bindingContext,
            LOOP_RANGE_ITERATOR_RESOLVED_CALL, loopRange,
            "No .iterator() function " + PsiDiagnosticUtils.atLocation(loopRange)
        )

        this.iteratorType = iteratorCall.resultingDescriptor.returnType!!
        this.asmTypeForIterator = codegen.asmType(iteratorType)

        this.nextCall = getNotNull(
            bindingContext,
            LOOP_RANGE_NEXT_RESOLVED_CALL, loopRange,
            "No next() function " + PsiDiagnosticUtils.atLocation(loopRange)
        )
    }

    override fun beforeLoop() {
        super.beforeLoop()

        // Iterator<E> tmp<iterator> = c.iterator()
        iteratorVarIndex = createLoopTempVariable(asmTypeForIterator)
        StackValue
            .local(iteratorVarIndex, asmTypeForIterator, iteratorType)
            .store(codegen.invokeFunction(iteratorCall, StackValue.none()), v)
    }

    override fun checkEmptyLoop(loopExit: Label) {}

    override fun checkPreCondition(loopExit: Label) {
        // tmp<iterator>.hasNext()

        val loopRange = forExpression.loopRange
        val hasNextCall = getNotNull(
            codegen.bindingContext, LOOP_RANGE_HAS_NEXT_RESOLVED_CALL,
            loopRange!!,
            "No hasNext() function " + PsiDiagnosticUtils.atLocation(loopRange)
        )
        val fakeCall = codegen.makeFakeCall(TransientReceiver(iteratorCall.resultingDescriptor.returnType!!))
        val result = codegen.invokeFunction(fakeCall, hasNextCall, StackValue.local(iteratorVarIndex, asmTypeForIterator, iteratorType))
        result.put(Type.BOOLEAN_TYPE, v)

        v.ifeq(loopExit)
    }

    override fun assignToLoopParameter() {
        val fakeCall = codegen.makeFakeCall(TransientReceiver(iteratorCall.resultingDescriptor.returnType!!))
        val value = codegen.invokeFunction(fakeCall, nextCall, StackValue.local(iteratorVarIndex, asmTypeForIterator, iteratorType))

        StackValue.local(loopParameterVar, loopParameterType, loopParameterKotlinType).store(value, v)
    }

    override fun checkPostConditionAndIncrement(loopExit: Label) {}
}
