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

package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.coroutines.isSuspendLambda
import org.jetbrains.kotlin.descriptors.*

internal val CallableDescriptor.isSuspend: Boolean
    get() = this is FunctionDescriptor && isSuspend

/**
 * @return naturally-ordered list of all parameters available inside the function body.
 */
internal val CallableDescriptor.allParameters: List<ParameterDescriptor>
    get() = if (this is ConstructorDescriptor) {
        listOf(this.constructedClass.thisAsReceiverParameter) + explicitParameters
    } else {
        explicitParameters
    }

/**
 * @return naturally-ordered list of the parameters that can have values specified at call site.
 */
internal val CallableDescriptor.explicitParameters: List<ParameterDescriptor>
    get() {
        val result = ArrayList<ParameterDescriptor>(valueParameters.size + 2)

        this.dispatchReceiverParameter?.let {
            result.add(it)
        }

        this.extensionReceiverParameter?.let {
            result.add(it)
        }

        result.addAll(valueParameters)

        return result
    }

/**
 * Returns the parameter in the original function corresponding to given parameter of this function.
 *
 * Note: `parameter.original` doesn't seem to be always the parameter of `this.original`.
 *
 * @param parameter must be declared in this function
 */
fun CallableDescriptor.getOriginalParameter(parameter: ParameterDescriptor): ParameterDescriptor = when (parameter) {
    is ValueParameterDescriptor -> this.original.valueParameters[parameter.index]
    this.dispatchReceiverParameter -> this.original.dispatchReceiverParameter!!
    this.extensionReceiverParameter -> this.original.extensionReceiverParameter!!
    else -> TODO("$parameter in $this")
}
