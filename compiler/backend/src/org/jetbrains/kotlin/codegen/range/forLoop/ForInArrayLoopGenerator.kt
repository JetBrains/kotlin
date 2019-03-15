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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.AsmUtil.boxType
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

class ForInArrayLoopGenerator(
    codegen: ExpressionCodegen,
    forExpression: KtForExpression,
    private val canCacheArrayLength: Boolean,
    private val shouldAlwaysStoreArrayInNewVar: Boolean
) : AbstractForLoopGenerator(codegen, forExpression) {
    private var indexVar: Int = 0
    private var arrayVar: Int = 0
    private var arrayLengthVar: Int = 0
    private val loopRangeType: KotlinType = bindingContext.getType(forExpression.loopRange!!)!!

    override fun beforeLoop() {
        super.beforeLoop()

        indexVar = createLoopTempVariable(Type.INT_TYPE)

        val loopRange = forExpression.loopRange
        val value = codegen.gen(loopRange)
        val asmLoopRangeType = codegen.asmType(loopRangeType)
        if (!shouldAlwaysStoreArrayInNewVar && value is StackValue.Local && value.type == asmLoopRangeType) {
            arrayVar = value.index // no need to copy local variable into another variable
        } else {
            arrayVar = createLoopTempVariable(OBJECT_TYPE)
            value.put(asmLoopRangeType, loopRangeType, v)
            v.store(arrayVar, OBJECT_TYPE)
        }

        if (canCacheArrayLength) {
            arrayLengthVar = createLoopTempVariable(Type.INT_TYPE)
            v.load(arrayVar, OBJECT_TYPE)
            v.arraylength()
            v.store(arrayLengthVar, Type.INT_TYPE)
        }

        v.iconst(0)
        v.store(indexVar, Type.INT_TYPE)
    }

    override fun checkEmptyLoop(loopExit: Label) {}

    override fun checkPreCondition(loopExit: Label) {
        v.load(indexVar, Type.INT_TYPE)
        if (canCacheArrayLength) {
            v.load(arrayLengthVar, Type.INT_TYPE)
        } else {
            v.load(arrayVar, OBJECT_TYPE)
            v.arraylength()
        }
        v.ificmpge(loopExit)
    }

    override fun assignToLoopParameter() {
        val arrayElParamType =
            if (KotlinBuiltIns.isArray(loopRangeType)) boxType(asmElementType, elementType, codegen.state.typeMapper) else asmElementType

        v.load(arrayVar, OBJECT_TYPE)
        v.load(indexVar, Type.INT_TYPE)
        v.aload(arrayElParamType)
        StackValue.onStack(arrayElParamType, elementType).put(asmElementType, elementType, codegen.v)
        v.store(loopParameterVar, asmElementType)
    }

    override fun checkPostConditionAndIncrement(loopExit: Label) {
        v.iinc(indexVar, 1)
    }
}
