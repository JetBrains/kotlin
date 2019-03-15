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

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

fun PropertyDescriptorImpl.initialize(
        type: KotlinType,
        typeParameters: List<TypeParameterDescriptor> = emptyList(),
        dispatchReceiverParameter: ReceiverParameterDescriptor? = null,
        extensionReceiverParameter: ReceiverParameterDescriptor? = null,
        getter: PropertyGetterDescriptorImpl? = null,
        setter: PropertySetterDescriptorImpl? = null,
        backingField: FieldDescriptor? = null,
        delegateField: FieldDescriptor? = null
): PropertyDescriptorImpl {
    setType(type, typeParameters, dispatchReceiverParameter, extensionReceiverParameter)
    initialize(getter, setter, backingField, delegateField)
    return this
}

fun CallableMemberDescriptor.createValueParameter(index: Int, name: String, type: KotlinType): ValueParameterDescriptor =
        ValueParameterDescriptorImpl(
                this, null,
                index,
                Annotations.EMPTY,
                Name.identifier(name),
                type,
                false, false, false, null, SourceElement.NO_SOURCE
        )
