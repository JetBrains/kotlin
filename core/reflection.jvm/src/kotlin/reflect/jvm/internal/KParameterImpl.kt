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

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import kotlin.reflect.KParameter
import kotlin.reflect.KType

internal class KParameterImpl(
        val callable: KCallableImpl<*>,
        override val index: Int,
        override val kind: KParameter.Kind,
        computeDescriptor: () -> ParameterDescriptor
) : KParameter, KAnnotatedElementImpl {
    private val descriptor: ParameterDescriptor by ReflectProperties.lazySoft(computeDescriptor)

    override val annotated: Annotated get() = descriptor

    override val name: String? get() {
        val valueParameter = descriptor as? ValueParameterDescriptor ?: return null
        if (valueParameter.containingDeclaration.hasSynthesizedParameterNames()) return null
        val name = valueParameter.name
        return if (name.isSpecial) null else name.asString()
    }

    override val type: KType
        get() = KTypeImpl(descriptor.type) { callable.caller.parameterTypes[index] }

    override val isOptional: Boolean
        get() = (descriptor as? ValueParameterDescriptor)?.hasDefaultValue() ?: false

    override fun equals(other: Any?) =
            other is KParameterImpl && callable == other.callable && descriptor == other.descriptor

    override fun hashCode() =
            (callable.hashCode() * 31) + descriptor.hashCode()

    override fun toString() =
            ReflectionObjectRenderer.renderParameter(this)
}
