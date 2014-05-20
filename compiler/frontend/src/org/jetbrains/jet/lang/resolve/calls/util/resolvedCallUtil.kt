/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.calls.util

import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentUnmapped
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentMatch
import org.jetbrains.jet.lang.resolve.calls.model.ArgumentMatchStatus
import org.jetbrains.jet.lang.psi.Call
import org.jetbrains.jet.lang.psi.ValueArgument

public fun <D : CallableDescriptor> ResolvedCall<D>.noErrorsInValueArguments(): Boolean {
    return getCall().getValueArguments().all { argument -> !getArgumentMapping(argument!!).isError() }
}

public fun <D : CallableDescriptor> ResolvedCall<D>.hasUnmappedArguments(): Boolean {
    return getCall().getValueArguments().any { argument -> getArgumentMapping(argument!!) == ArgumentUnmapped }
}

public fun <D : CallableDescriptor> ResolvedCall<D>.hasUnmappedParameters(): Boolean {
    val parameterToArgumentMap = getValueArguments()
    return !parameterToArgumentMap.keySet().containsAll(getResultingDescriptor().getValueParameters())
}

public fun <D : CallableDescriptor> ResolvedCall<D>.hasTypeMismatchErrorOnParameter(parameter: ValueParameterDescriptor): Boolean {
    val resolvedValueArgument = getValueArguments()[parameter]
    if (resolvedValueArgument == null) return true

    return resolvedValueArgument.getArguments().any { argument ->
        val argumentMapping = getArgumentMapping(argument)
        argumentMapping is ArgumentMatch && argumentMapping.status == ArgumentMatchStatus.TYPE_MISMATCH
    }
}

fun Call.getAllValueArguments(): List<ValueArgument> {
    val arguments = getValueArguments() +
                    getFunctionLiteralArguments().map { functionLiteral -> CallMaker.makeValueArgument(functionLiteral) }
    [suppress("UNCHECKED_CAST")]
    return arguments as List<ValueArgument>
}
