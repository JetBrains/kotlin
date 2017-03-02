package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor

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
