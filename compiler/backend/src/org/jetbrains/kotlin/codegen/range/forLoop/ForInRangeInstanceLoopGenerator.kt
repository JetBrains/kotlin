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
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.range.comparison.ComparisonGenerator
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.types.KotlinType

class ForInRangeInstanceLoopGenerator(
    codegen: ExpressionCodegen,
    forExpression: KtForExpression,
    private val rangeExpression: KtExpression,
    comparisonGenerator: ComparisonGenerator,
    private val reversed: Boolean
) : AbstractForInRangeLoopGenerator(codegen, forExpression, if (reversed) -1 else 1, comparisonGenerator) {

    override fun storeRangeStartAndEnd() {
        val asmLoopRangeType = codegen.asmType(rangeKotlinType)
        codegen.gen(rangeExpression, asmLoopRangeType, rangeKotlinType)
        v.dup()

        val firstName = rangeKotlinType.getPropertyGetterName("first")
        val lastName = rangeKotlinType.getPropertyGetterName("last")

        // ranges inherit first and last from corresponding progressions
        if (reversed) {
            generateRangeOrProgressionProperty(asmLoopRangeType, lastName, asmElementType, loopParameterType, loopParameterVar)
            generateRangeOrProgressionProperty(asmLoopRangeType, firstName, asmElementType, asmElementType, endVar)
        } else {
            generateRangeOrProgressionProperty(asmLoopRangeType, firstName, asmElementType, loopParameterType, loopParameterVar)
            generateRangeOrProgressionProperty(asmLoopRangeType, lastName, asmElementType, asmElementType, endVar)
        }
    }

}
