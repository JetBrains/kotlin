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
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

abstract class AbstractForInProgressionOrRangeLoopGenerator(codegen: ExpressionCodegen, forExpression: KtForExpression) :
    AbstractForLoopGenerator(codegen, forExpression) {
    protected var endVar: Int = -1

    private var loopParameter: StackValue? = null

    init {
        assert(
            asmElementType.sort == Type.INT ||
                    asmElementType.sort == Type.BYTE ||
                    asmElementType.sort == Type.SHORT ||
                    asmElementType.sort == Type.CHAR ||
                    asmElementType.sort == Type.LONG
        ) {
            "Unexpected range element type: " + asmElementType
        }
    }

    override fun beforeLoop() {
        super.beforeLoop()

        endVar = createLoopTempVariable(asmElementType)
    }

    protected fun checkPostCondition(loopExit: Label) {
        assert(endVar != -1) {
            "endVar must be allocated, endVar = " + endVar
        }
        loopParameter().put(asmElementType, elementType, v)
        v.load(endVar, asmElementType)
        if (asmElementType.sort == Type.LONG) {
            v.lcmp()
            v.ifeq(loopExit)
        } else {
            v.ificmpeq(loopExit)
        }
    }

    override fun checkPreCondition(loopExit: Label) {}

    protected fun loopParameter(): StackValue =
        loopParameter ?: StackValue.local(loopParameterVar, loopParameterType).also { loopParameter = it }
}
