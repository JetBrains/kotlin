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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrSyntheticStaticPropertyGetterDescriptorImpl
import org.jetbrains.kotlin.ir.descriptors.IrSyntheticStaticPropertySetterDescriptorImpl
import org.jetbrains.kotlin.storage.StorageManager


class SyntheticDescriptorsFactory(storageManager: StorageManager) {
    private val propertyGetters = storageManager.createMemoizedFunction<PropertyDescriptor, PropertyGetterDescriptor> {
        property ->
        when {
            isStaticPropertyInClass(property) ->
                IrSyntheticStaticPropertyGetterDescriptorImpl(property)
            else ->
                throw AssertionError("Don't know how to create synthetic getter for $property")
        }
    }

    private val propertySetters = storageManager.createMemoizedFunction<PropertyDescriptor, PropertySetterDescriptor> {
        property ->
        when {
            isStaticPropertyInClass(property) ->
                IrSyntheticStaticPropertySetterDescriptorImpl(property)
            else ->
                throw AssertionError("Don't know how to create synthetic setter for $property")
        }
    }

    private fun isStaticPropertyInClass(propertyDescriptor: PropertyDescriptor): Boolean =
            propertyDescriptor.containingDeclaration is ClassDescriptor &&
            propertyDescriptor.dispatchReceiverParameter == null &&
            propertyDescriptor.extensionReceiverParameter == null

    fun getOrCreatePropertyGetter(propertyDescriptor: PropertyDescriptor): PropertyGetterDescriptor =
            propertyGetters(propertyDescriptor)

    fun getOrCreatePropertySetter(propertyDescriptor: PropertyDescriptor): PropertySetterDescriptor =
            propertySetters(propertyDescriptor)
}
