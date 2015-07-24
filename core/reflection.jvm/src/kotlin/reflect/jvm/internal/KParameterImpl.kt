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
import kotlin.reflect.KParameter
import kotlin.reflect.KType

class KParameterImpl(
        override val index: Int,
        private val computeDescriptor: () -> ParameterDescriptor
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
        get() = KTypeImpl(descriptor.getType())
}
