/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.TypeSubstitutor

interface ComposableCallableDescriptor : CallableDescriptor {
    val underlyingDescriptor: CallableDescriptor
}

interface ComposableFunctionDescriptor : FunctionDescriptor, ComposableCallableDescriptor {
    override val underlyingDescriptor: FunctionDescriptor
}

interface ComposablePropertyDescriptor : PropertyDescriptor, ComposableCallableDescriptor {
    override val underlyingDescriptor: PropertyDescriptor
}

class ComposablePropertyDescriptorImpl(
    override val underlyingDescriptor: PropertyDescriptor
) : PropertyDescriptor by underlyingDescriptor, ComposablePropertyDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor? {
        return underlyingDescriptor.substitute(substitutor)?.let {
            ComposablePropertyDescriptorImpl(it)
        }
    }
}

fun ComposableFunctionDescriptor(
    underlyingDescriptor: FunctionDescriptor
): ComposableFunctionDescriptor {
    return if (underlyingDescriptor is SimpleFunctionDescriptor) {
        ComposableSimpleFunctionDescriptorImpl(underlyingDescriptor)
    } else {
        ComposableFunctionDescriptorImpl(underlyingDescriptor)
    }
}

class ComposableFunctionDescriptorImpl(
    override val underlyingDescriptor: FunctionDescriptor
) : FunctionDescriptor by underlyingDescriptor, ComposableFunctionDescriptor {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor? {
        return underlyingDescriptor.substitute(substitutor)?.let {
            ComposableFunctionDescriptor(it)
        }
    }
}

class ComposableSimpleFunctionDescriptorImpl(
    override val underlyingDescriptor: SimpleFunctionDescriptor
) : SimpleFunctionDescriptor by underlyingDescriptor, ComposableFunctionDescriptor,
    DescriptorWithContainerSource {
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor? {
        return underlyingDescriptor.substitute(substitutor)?.let {
            ComposableFunctionDescriptor(it)
        }
    }

    override fun getSource(): SourceElement {
        return underlyingDescriptor.source
    }

    override val containerSource: DeserializedContainerSource?
        get() = (underlyingDescriptor as DescriptorWithContainerSource).containerSource

    override fun toString(): String {
        return "ComposableSimpleFunctionDescriptorImpl(${this.underlyingDescriptor.name})"
    }
}
