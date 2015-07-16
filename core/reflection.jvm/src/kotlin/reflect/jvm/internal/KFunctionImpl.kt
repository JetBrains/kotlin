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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import kotlin.jvm.internal.FunctionImpl
import kotlin.reflect.*

open class KFunctionImpl protected constructor(
        container: KCallableContainerImpl,
        name: String,
        signature: String,
        descriptorInitialValue: FunctionDescriptor?
) : KFunction<Any?>, FunctionImpl(),
        KLocalFunction<Any?>, KMemberFunction<Any, Any?>, KTopLevelExtensionFunction<Any?, Any?>, KTopLevelFunction<Any?> {
    constructor(container: KCallableContainerImpl, name: String, signature: String) : this(container, name, signature, null)

    constructor(container: KCallableContainerImpl, descriptor: FunctionDescriptor) : this(
            container, descriptor.getName().asString(), RuntimeTypeMapper.mapSignature(descriptor), descriptor
    )

    protected val descriptor: FunctionDescriptor by ReflectProperties.lazySoft<FunctionDescriptor>(descriptorInitialValue) {
        container.findFunctionDescriptor(name, signature)
    }

    override val name: String get() = descriptor.getName().asString()

    override fun getArity(): Int {
        // TODO: test?
        return descriptor.getValueParameters().size() +
               (if (descriptor.getDispatchReceiverParameter() != null) 1 else 0) +
               (if (descriptor.getExtensionReceiverParameter() != null) 1 else 0)
    }

    override fun equals(other: Any?): Boolean =
            other is KFunctionImpl && descriptor == other.descriptor

    override fun hashCode(): Int =
            descriptor.hashCode()

    override fun toString(): String =
            ReflectionObjectRenderer.renderFunction(descriptor)
}
