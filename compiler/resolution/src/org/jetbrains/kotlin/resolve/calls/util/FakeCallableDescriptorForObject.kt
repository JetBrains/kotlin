/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.resolve.descriptorUtil.classValueType
import org.jetbrains.kotlin.resolve.descriptorUtil.getClassObjectReferenceTarget
import org.jetbrains.kotlin.resolve.descriptorUtil.hasClassValueDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import java.util.*

open class FakeCallableDescriptorForObject(
    val classDescriptor: ClassDescriptor
) : DeclarationDescriptorWithVisibility by classDescriptor.getClassObjectReferenceTarget(), VariableDescriptor {

    init {
        assert(classDescriptor.hasClassValueDescriptor) {
            "FakeCallableDescriptorForObject can be created only for objects, classes with companion object or enum entries: $classDescriptor"
        }

    }

    open fun getReferencedDescriptor(): ClassifierDescriptorWithTypeParameters = classDescriptor.getClassObjectReferenceTarget()

    fun getReferencedObject(): ClassDescriptor = classDescriptor.getClassObjectReferenceTarget()

    override fun getContextReceiverParameters(): List<ReceiverParameterDescriptor> = emptyList()

    override fun getExtensionReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? = null

    override fun getTypeParameters(): List<TypeParameterDescriptor> = Collections.emptyList()

    override fun getValueParameters(): List<ValueParameterDescriptor> = Collections.emptyList()

    override fun getReturnType(): KotlinType? = type

    override fun hasSynthesizedParameterNames() = false

    override fun hasStableParameterNames() = false

    override fun getOverriddenDescriptors(): Set<CallableDescriptor> = Collections.emptySet()

    override fun getType(): KotlinType = classDescriptor.classValueType!!

    override fun isVar() = false

    override fun getOriginal(): CallableDescriptor = this

    override fun getCompileTimeInitializer() = null

    override fun getSource(): SourceElement = classDescriptor.source

    override fun isConst(): Boolean = false

    override fun isLateInit(): Boolean = false

    override fun equals(other: Any?) = other is FakeCallableDescriptorForObject && classDescriptor == other.classDescriptor

    override fun hashCode() = classDescriptor.hashCode()

    override fun getContainingDeclaration() = classDescriptor.getClassObjectReferenceTarget().containingDeclaration

    override fun substitute(substitutor: TypeSubstitutor) = this

    override fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>?): V? = null
}
