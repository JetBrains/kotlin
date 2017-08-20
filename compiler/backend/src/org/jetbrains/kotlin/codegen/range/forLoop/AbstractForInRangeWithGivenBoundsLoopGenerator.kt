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

abstract class AbstractForInRangeWithGivenBoundsLoopGenerator(
        codegen: ExpressionCodegen,
        forExpression: KtForExpression,
        step: Int = 1
) : AbstractForInRangeLoopGenerator(codegen, forExpression, step) {
    protected abstract fun generateFrom(): StackValue
    protected abstract fun generateTo(): StackValue

    override fun storeRangeStartAndEnd() {
        loopParameter().store(generateFrom(), v)
        StackValue.local(endVar, asmElementType).store(generateTo(), v)
    }
}