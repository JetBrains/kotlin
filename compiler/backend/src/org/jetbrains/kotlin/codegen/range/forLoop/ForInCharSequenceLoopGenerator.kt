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
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class ForInCharSequenceLoopGenerator(
    codegen: ExpressionCodegen,
    forExpression: KtForExpression,
    private val canCacheLength: Boolean,
    private val charSequenceClassType: Type?
) : AbstractForLoopGenerator(codegen, forExpression) {
    private var indexVar: Int = 0
    private var charSequenceVar: Int = 0
    private var charSequenceLengthVar: Int = 0

    private val charSequenceType = charSequenceClassType ?: CHAR_SEQUENCE_TYPE

    override fun beforeLoop() {
        super.beforeLoop()

        indexVar = createLoopTempVariable(Type.INT_TYPE)

        val loopRange = forExpression.loopRange
        val value = codegen.gen(loopRange)
        val loopRangeType: KotlinType = bindingContext.getType(forExpression.loopRange!!)!!
        val asmLoopRangeType = codegen.asmType(loopRangeType)

        // NB even if we already have a loop range stored in local variable, that variable might be modified in the loop body
        // (see controlStructures/forInCharSequenceMut.kt).
        // We should always store the corresponding CharSequence to a local variable to preserve the Iterator-based behavior.
        charSequenceVar = createLoopTempVariable(charSequenceType)
        value.put(asmLoopRangeType, loopRangeType, v)
        v.store(charSequenceVar, charSequenceType)

        if (canCacheLength) {
            charSequenceLengthVar = createLoopTempVariable(Type.INT_TYPE)
            v.load(charSequenceVar, charSequenceType)
            v.invokeCharSequenceMethod("length", "()I")
            v.store(charSequenceLengthVar, Type.INT_TYPE)
        }

        v.iconst(0)
        v.store(indexVar, Type.INT_TYPE)
    }

    override fun checkEmptyLoop(loopExit: Label) {}

    override fun checkPreCondition(loopExit: Label) {
        v.load(indexVar, Type.INT_TYPE)
        if (canCacheLength) {
            v.load(charSequenceLengthVar, Type.INT_TYPE)
        } else {
            v.load(charSequenceVar, charSequenceType)
            v.invokeCharSequenceMethod("length", "()I")
        }
        v.ificmpge(loopExit)
    }

    override fun assignToLoopParameter() {
        v.load(charSequenceVar, charSequenceType)
        v.load(indexVar, Type.INT_TYPE)
        v.invokeCharSequenceMethod("charAt", "(I)C")
        StackValue.onStack(Type.CHAR_TYPE).put(loopParameterType, loopParameterKotlinType, codegen.v)
        v.store(loopParameterVar, loopParameterType)
    }

    override fun checkPostConditionAndIncrement(loopExit: Label) {
        v.iinc(indexVar, 1)
    }

    private fun InstructionAdapter.invokeCharSequenceMethod(name: String, desc: String) {
        val charSequenceClassType = charSequenceClassType
        if (charSequenceClassType != null) {
            invokevirtual(charSequenceClassType.internalName, name, desc, false)
        } else {
            invokeinterface(CHAR_SEQUENCE_TYPE.internalName, name, desc)
        }
    }

    companion object {
        val CHAR_SEQUENCE_TYPE: Type = Type.getObjectType("java/lang/CharSequence")
    }
}
