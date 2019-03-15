/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.range.forLoop

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

class IteratorWithIndexForLoopGenerator(
    codegen: ExpressionCodegen,
    forExpression: KtForExpression,
    loopParameter: KtDestructuringDeclaration,
    rangeCall: ResolvedCall<out CallableDescriptor>,
    private val iteratorOwnerType: Type
) : AbstractWithIndexForLoopGenerator(codegen, forExpression, loopParameter, rangeCall) {
    private var iteratorVar = -1

    private var indexVar = -1
    private var indexType = Type.INT_TYPE

    private val asmTypeForIterator = Type.getType(Iterator::class.java)

    override fun beforeLoop() {
        indexVar = indexLoopComponent?.parameterVar ?: createLoopTempVariable(Type.INT_TYPE)
        indexType = indexLoopComponent?.parameterType ?: Type.INT_TYPE

        StackValue.local(indexVar, indexType).store(StackValue.constant(0), v)

        iteratorVar = createLoopTempVariable(asmTypeForIterator)
        codegen.generateCallReceiver(rangeCall).put(iteratorOwnerType, v)
        v.invokeinterface(iteratorOwnerType.internalName, "iterator", "()Ljava/util/Iterator;")
        StackValue.local(iteratorVar, asmTypeForIterator).store(StackValue.onStack(asmTypeForIterator), v)
    }

    override fun checkPreCondition(loopExit: Label) {
        v.load(iteratorVar, asmTypeForIterator)
        v.invokeinterface("java/util/Iterator", "hasNext", "()Z")
        v.ifeq(loopExit)
    }

    override fun assignLoopParametersNextValues() {
        v.load(iteratorVar, asmTypeForIterator)
        v.invokeinterface("java/util/Iterator", "next", "()Ljava/lang/Object;")
        if (elementLoopComponent != null) {
            StackValue.local(elementLoopComponent.parameterVar, elementLoopComponent.parameterType)
                .store(StackValue.onStack(AsmTypes.OBJECT_TYPE), v)
        } else {
            v.pop()
        }
    }

    override fun incrementAndCheckPostCondition(loopExit: Label) {
        v.iinc(indexVar, 1)
    }
}