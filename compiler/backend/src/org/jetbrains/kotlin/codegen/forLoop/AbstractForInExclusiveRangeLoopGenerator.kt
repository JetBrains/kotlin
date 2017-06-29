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

package org.jetbrains.kotlin.codegen.forLoop

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

abstract class AbstractForInExclusiveRangeLoopGenerator(
        codegen: ExpressionCodegen,
        forExpression: KtForExpression
) : AbstractForInRangeWithGivenBoundsLoopGenerator(codegen, forExpression) {
    override fun checkEmptyLoop(loopExit: Label) {}

    override fun checkPreCondition(loopExit: Label) {
        loopParameter().put(asmElementType, v)
        v.load(endVar, asmElementType)
        if (asmElementType.sort == Type.LONG) {
            v.lcmp()
            v.ifge(loopExit)
        }
        else {
            v.ificmpge(loopExit)
        }
    }

    override fun checkPostConditionAndIncrement(loopExit: Label) {
        incrementLoopVariable()
    }
}
