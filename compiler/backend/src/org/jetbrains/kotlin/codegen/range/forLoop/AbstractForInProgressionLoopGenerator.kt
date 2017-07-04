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
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

abstract class AbstractForInProgressionLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression)
    : AbstractForInProgressionOrRangeLoopGenerator(codegen, forExpression)
{
    protected var incrementVar: Int = -1
    protected val asmLoopRangeType: Type
    protected val incrementType: Type

    init {
        val loopRangeType = bindingContext.getType(forExpression.loopRange!!)!!
        asmLoopRangeType = codegen.asmType(loopRangeType)

        val incrementProp = loopRangeType.memberScope.getContributedVariables(Name.identifier("step"), NoLookupLocation.FROM_BACKEND)
        assert(incrementProp.size == 1) { loopRangeType.toString() + " " + incrementProp.size }
        incrementType = codegen.asmType(incrementProp.iterator().next().type)
    }

    override fun beforeLoop() {
        super.beforeLoop()

        incrementVar = createLoopTempVariable(asmElementType)

        storeProgressionParametersToLocalVars()
    }

    protected abstract fun storeProgressionParametersToLocalVars()

    override fun checkEmptyLoop(loopExit: Label) {
        loopParameter().put(asmElementType, v)
        v.load(endVar, asmElementType)
        v.load(incrementVar, incrementType)

        val negativeIncrement = Label()
        val afterIf = Label()

        if (asmElementType.sort == Type.LONG) {
            v.lconst(0L)
            v.lcmp()
            v.ifle(negativeIncrement) // if increment < 0, jump

            // increment > 0
            v.lcmp()
            v.ifgt(loopExit)
            v.goTo(afterIf)

            // increment < 0
            v.mark(negativeIncrement)
            v.lcmp()
            v.iflt(loopExit)
            v.mark(afterIf)
        }
        else {
            v.ifle(negativeIncrement) // if increment < 0, jump

            // increment > 0
            v.ificmpgt(loopExit)
            v.goTo(afterIf)

            // increment < 0
            v.mark(negativeIncrement)
            v.ificmplt(loopExit)
            v.mark(afterIf)
        }
    }

    override fun assignToLoopParameter() {}

    override fun checkPostConditionAndIncrement(loopExit: Label) {
        checkPostCondition(loopExit)

        val loopParameter = loopParameter()
        loopParameter.put(asmElementType, v)
        v.load(incrementVar, asmElementType)
        v.add(asmElementType)

        if (asmElementType === Type.BYTE_TYPE || asmElementType === Type.SHORT_TYPE || asmElementType === Type.CHAR_TYPE) {
            StackValue.coerce(Type.INT_TYPE, asmElementType, v)
        }

        loopParameter.store(StackValue.onStack(asmElementType), v)
    }
}