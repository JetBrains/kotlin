/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.range.forLoop

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.generateCallReceiver
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

class CharSequenceWithIndexForLoopGenerator(
    codegen: ExpressionCodegen,
    forExpression: KtForExpression,
    loopParameter: KtDestructuringDeclaration,
    rangeCall: ResolvedCall<out CallableDescriptor>,
    private val canCacheLength: Boolean
) : AbstractWithIndexForLoopGenerator(codegen, forExpression, loopParameter, rangeCall) {

    private val charSeqType = codegen.asmType(ExpressionCodegen.getExpectedReceiverType(rangeCall))
    private var charSeqVar = -1
    private var lengthVar = -1

    private var indexVar = -1
    private var indexType = Type.INT_TYPE

    override fun beforeLoop() {
        charSeqVar = createLoopTempVariable(charSeqType)
        val charSeqValue = StackValue.local(charSeqVar, charSeqType)
        charSeqValue.store(codegen.generateCallReceiver(rangeCall), v)

        if (canCacheLength) {
            lengthVar = createLoopTempVariable(Type.INT_TYPE)
            evalCharSeqLengthOnStack()
            v.store(lengthVar, Type.INT_TYPE)
        }

        indexVar = indexLoopComponent?.parameterVar ?: createLoopTempVariable(Type.INT_TYPE)
        indexType = indexLoopComponent?.parameterType ?: Type.INT_TYPE

        StackValue.local(indexVar, indexType).store(StackValue.constant(0), v)
    }

    private fun evalCharSeqLengthOnStack() {
        v.load(charSeqVar, charSeqType)
        v.invokeinterface("java/lang/CharSequence", "length", "()I")
    }

    override fun checkPreCondition(loopExit: Label) {
        v.load(indexVar, Type.INT_TYPE)
        if (canCacheLength) {
            v.load(lengthVar, Type.INT_TYPE)
        } else {
            evalCharSeqLengthOnStack()
        }
        v.ificmpge(loopExit)
    }

    override fun assignLoopParametersNextValues() {
        v.load(charSeqVar, charSeqType)
        v.load(indexVar, Type.INT_TYPE)
        v.invokeinterface("java/lang/CharSequence", "charAt", "(I)C")
        if (elementLoopComponent != null) {
            StackValue.local(elementLoopComponent.parameterVar, elementLoopComponent.parameterType)
                .store(StackValue.onStack(Type.CHAR_TYPE), v)
        } else {
            v.pop()
        }
    }

    override fun incrementAndCheckPostCondition(loopExit: Label) {
        v.iinc(indexVar, 1)
    }

}