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

package org.jetbrains.kotlin.resolve.calls.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.descriptorUtil.classObjectType
import org.jetbrains.kotlin.resolve.descriptorUtil.getClassObjectReferenceTarget
import org.jetbrains.kotlin.resolve.descriptorUtil.hasClassObjectType
import org.jetbrains.kotlin.types.JetType
import java.util.Collections

public class FakeCallableDescriptorForObject(
        public val classDescriptor: ClassDescriptor
) : DeclarationDescriptorWithVisibility by classDescriptor.getClassObjectReferenceTarget(), VariableDescriptor {

    init {
        assert(classDescriptor.hasClassObjectType) {
            "FakeCallableDescriptorForObject can be created only for objects, classes with companion object or enum entries: $classDescriptor"
        }

    }

    public fun getReferencedDescriptor(): ClassDescriptor = classDescriptor.getClassObjectReferenceTarget()

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getTypeParameters(): List<TypeParameterDescriptor> = Collections.emptyList()

    override fun getValueParameters(): List<ValueParameterDescriptor> = Collections.emptyList()

    override fun getReturnType(): JetType? = getType()

    override fun hasSynthesizedParameterNames() = false

    override fun hasStableParameterNames() = false

    override fun getOverriddenDescriptors(): Set<CallableDescriptor> = Collections.emptySet()

    override fun getType(): JetType = classDescriptor.classObjectType!!

    override fun isVar() = false

    override fun getOriginal(): CallableDescriptor = this

    override fun getCompileTimeInitializer(): CompileTimeConstant<out Any?>? = null

    override fun getSource(): SourceElement = classDescriptor.getSource()
}
