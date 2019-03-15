/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.range.forLoop

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.generateCallReceiver
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

class ArrayWithIndexForLoopGenerator(
    codegen: ExpressionCodegen,
    forExpression: KtForExpression,
    loopParameter: KtDestructuringDeclaration,
    rangeCall: ResolvedCall<out CallableDescriptor>
) : AbstractWithIndexForLoopGenerator(codegen, forExpression, loopParameter, rangeCall) {

    private val arrayKotlinType = ExpressionCodegen.getExpectedReceiverType(rangeCall)
    private val arrayType = codegen.asmType(arrayKotlinType)
    private val arrayElementType = AsmUtil.correctElementType(arrayType)
    private var arrayVar = -1
    private var arrayLengthVar = -1

    private var indexVar = -1
    private var indexType = Type.INT_TYPE

    override fun beforeLoop() {
        arrayVar = createLoopTempVariable(arrayType)

        arrayLengthVar = createLoopTempVariable(Type.INT_TYPE)

        indexVar = indexLoopComponent?.parameterVar ?: createLoopTempVariable(Type.INT_TYPE)
        indexType = indexLoopComponent?.parameterType ?: Type.INT_TYPE

        val arrayValue = StackValue.local(arrayVar, arrayType)
        arrayValue.store(codegen.generateCallReceiver(rangeCall), v)

        arrayValue.put(arrayType, arrayKotlinType, v)
        v.arraylength()
        v.store(arrayLengthVar, Type.INT_TYPE)

        StackValue.local(indexVar, indexType).store(StackValue.constant(0), v)
    }

    override fun checkPreCondition(loopExit: Label) {
        StackValue.local(indexVar, indexType)
            .put(Type.INT_TYPE, v)
        v.load(arrayLengthVar, Type.INT_TYPE)
        v.ificmpge(loopExit)
    }

    override fun assignLoopParametersNextValues() {
        if (elementLoopComponent != null) {
            v.load(arrayVar, AsmTypes.OBJECT_TYPE)
            v.load(indexVar, Type.INT_TYPE)
            v.aload(arrayElementType)
            StackValue.local(elementLoopComponent.parameterVar, elementLoopComponent.parameterType)
                .store(StackValue.onStack(arrayElementType), v)
        }
    }

    override fun incrementAndCheckPostCondition(loopExit: Label) {
        v.iinc(indexVar, 1)
    }
}