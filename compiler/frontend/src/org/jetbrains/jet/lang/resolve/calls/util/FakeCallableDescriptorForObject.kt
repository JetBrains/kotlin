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
import org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import java.util.Collections
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant
import org.jetbrains.jet.lang.descriptors.SourceElement
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.lang.resolve.descriptorUtil.getClassObjectReferenceTarget

public class FakeCallableDescriptorForObject(
        private val classDescriptor: ClassDescriptor
) : DeclarationDescriptorWithVisibility by classDescriptor.getClassObjectReferenceTarget(), VariableDescriptor {

    {
        assert(classDescriptor.getClassObjectType() != null) {
            "FakeCallableDescriptorForObject can be created only for objects, classes with class object or enum entries: $classDescriptor"
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

    override fun getType(): JetType = classDescriptor.getClassObjectType()!!

    override fun isVar() = false

    override fun getOriginal(): CallableDescriptor = this

    override fun getCompileTimeInitializer(): CompileTimeConstant<out Any?>? = null

    override fun getSource(): SourceElement = classDescriptor.getSource()
}
