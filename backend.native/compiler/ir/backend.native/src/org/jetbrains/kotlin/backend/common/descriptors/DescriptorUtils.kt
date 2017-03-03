package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor

/**
 * @return naturally-ordered list of all parameters available inside the function body.
 */
internal val CallableDescriptor.allParameters: List<ParameterDescriptor>
    get() {
        val receivers = mutableListOf<ParameterDescriptor>()

        if (this is ConstructorDescriptor)
            receivers.add(this.constructedClass.thisAsReceiverParameter)

        val dispatchReceiverParameter = this.dispatchReceiverParameter
        if (dispatchReceiverParameter != null)
            receivers.add(dispatchReceiverParameter)

        val extensionReceiverParameter = this.extensionReceiverParameter
        if (extensionReceiverParameter != null)
            receivers.add(extensionReceiverParameter)

        return receivers + this.valueParameters
    }