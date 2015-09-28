/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.psi.JetLoopExpression
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import java.util.*

/**
 * The purpose of this class is to find all variable assignments
 * **before** loop analysis
 */
class PreliminaryLoopVisitor private constructor() : AssignedVariablesSearcher() {

    fun clearDataFlowInfoForAssignedLocalVariables(dataFlowInfo: DataFlowInfo): DataFlowInfo {
        var resultFlowInfo = dataFlowInfo
        val nullabilityMap = resultFlowInfo.completeNullabilityInfo
        val valueSetToClear = LinkedHashSet<DataFlowValue>()
        for (value in nullabilityMap.keySet()) {
            // Only predictable variables are under interest here
            val id = value.id
            if (value.kind == DataFlowValue.Kind.PREDICTABLE_VARIABLE && id is LocalVariableDescriptor) {
                if (hasWriters(id)) {
                    valueSetToClear.add(value)
                }
            }
        }
        for (valueToClear in valueSetToClear) {
            resultFlowInfo = resultFlowInfo.clearValueInfo(valueToClear)
        }
        return resultFlowInfo
    }

    companion object {

        @JvmStatic
        fun visitLoop(loopExpression: JetLoopExpression): PreliminaryLoopVisitor {
            val visitor = PreliminaryLoopVisitor()
            loopExpression.accept(visitor, null)
            return visitor
        }
    }
}
