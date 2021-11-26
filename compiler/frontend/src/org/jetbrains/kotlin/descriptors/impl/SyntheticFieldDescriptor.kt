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

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name

class SyntheticFieldDescriptor private constructor(
    val propertyDescriptor: PropertyDescriptor,
    accessorDescriptor: PropertyAccessorDescriptor,
    sourceElement: SourceElement
) : LocalVariableDescriptor(
    accessorDescriptor, Annotations.EMPTY, SyntheticFieldDescriptor.NAME,
    propertyDescriptor.type, propertyDescriptor.isVar, false, false,
    sourceElement
) {
    constructor(
        accessorDescriptor: PropertyAccessorDescriptor,
        sourceElement: SourceElement
    ) : this(accessorDescriptor.correspondingProperty, accessorDescriptor, sourceElement)

    override fun getDispatchReceiverParameter() = null

    fun getDispatchReceiverForBackend() = getDispatchReceiverParameterForBackend()?.value

    fun getDispatchReceiverParameterForBackend() = propertyDescriptor.dispatchReceiverParameter

    companion object {
        @JvmField
        val NAME = Name.identifier("field")
    }
}

val DeclarationDescriptor.referencedProperty: PropertyDescriptor?
    get() = when (this) {
        is SyntheticFieldDescriptor -> this.propertyDescriptor
        is PropertyDescriptor -> this
        else -> null
    }
