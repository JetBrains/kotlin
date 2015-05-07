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

package org.jetbrains.kotlin.idea.core

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.FunctionLiteralArgument
import org.jetbrains.kotlin.psi.ValueArgument
import java.util.HashMap

public fun Call.mapArgumentsToParameters(targetDescriptor: CallableDescriptor): Map<ValueArgument, ValueParameterDescriptor> {
    val parameters = targetDescriptor.getValueParameters()
    if (parameters.isEmpty()) return emptyMap()

    val map = HashMap<ValueArgument, ValueParameterDescriptor>()
    val parametersByName = parameters.toMap { it.getName().asString() }

    var positionalArgumentIndex: Int? = 0

    for (argument in getValueArguments()) {
        if (argument is FunctionLiteralArgument) {
            map[argument] = parameters.last()
        }
        else {
            val argumentName = argument.getArgumentName()?.getReferenceExpression()?.getReferencedName()

            if (argumentName != null) {
                if (targetDescriptor.hasStableParameterNames()) {
                    val parameter = parametersByName[argumentName]
                    if (parameter != null) {
                        map[argument] = parameter
                    }
                }
                positionalArgumentIndex = null
            }
            else {
                if (positionalArgumentIndex != null && positionalArgumentIndex < parameters.size()) {
                    val parameter = parameters[positionalArgumentIndex]
                    map[argument] = parameter

                    if (parameter.getVarargElementType() == null || argument.getSpreadElement() != null) {
                        positionalArgumentIndex++
                    }
                }
            }
        }
    }

    return map
}
