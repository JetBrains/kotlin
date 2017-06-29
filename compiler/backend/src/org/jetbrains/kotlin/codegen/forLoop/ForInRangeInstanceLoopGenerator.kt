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

class ForInRangeInstanceLoopGenerator(
        codegen: ExpressionCodegen,
        forExpression: KtForExpression
) : AbstractForInRangeLoopGenerator(codegen, forExpression) {

    override fun storeRangeStartAndEnd() {
        val loopRangeType = codegen.bindingContext.getType(forExpression.loopRange!!)!!
        val asmLoopRangeType = codegen.asmType(loopRangeType)
        codegen.gen(forExpression.loopRange, asmLoopRangeType)
        v.dup()

        // ranges inherit first and last from corresponding progressions
        generateRangeOrProgressionProperty(asmLoopRangeType, "getFirst", asmElementType, loopParameterType, loopParameterVar)
        generateRangeOrProgressionProperty(asmLoopRangeType, "getLast", asmElementType, asmElementType, endVar)
    }
}
