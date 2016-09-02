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
import org.jetbrains.kotlin.ir.descriptors.IrSyntheticPropertyAccessorDescriptor.Kind.MEMBER_PROPERTY
import org.jetbrains.kotlin.ir.descriptors.IrSyntheticPropertyAccessorDescriptor.Kind.STATIC_PROPERTY
import org.jetbrains.kotlin.ir.descriptors.IrSyntheticPropertyGetterDescriptorImpl
import org.jetbrains.kotlin.ir.descriptors.IrSyntheticPropertySetterDescriptorImpl
import java.lang.AssertionError
import java.util.*


class SyntheticDescriptorsFactory {
    private val propertyGetters = HashMap<PropertyDescriptor, PropertyGetterDescriptor>()

    private fun generateGetter(property: PropertyDescriptor): PropertyGetterDescriptor {
        return when {
            isStaticPropertyInClass(property) ->
                IrSyntheticPropertyGetterDescriptorImpl(property, STATIC_PROPERTY)
            isPropertyInClass(property) ->
                IrSyntheticPropertyGetterDescriptorImpl(property, MEMBER_PROPERTY)
            else ->
                throw AssertionError("Don't know how to create synthetic getter for $property")
        }
    }

    private val propertySetters = HashMap<PropertyDescriptor, PropertySetterDescriptor>()

    private fun generateSetter(property: PropertyDescriptor): PropertySetterDescriptor {
        return when {
            isStaticPropertyInClass(property) ->
                IrSyntheticPropertySetterDescriptorImpl(property, STATIC_PROPERTY)
            isPropertyInClass(property) ->
                IrSyntheticPropertySetterDescriptorImpl(property, MEMBER_PROPERTY)
            else ->
                throw AssertionError("Don't know how to create synthetic setter for $property")
        }
    }

    private fun isStaticPropertyInClass(propertyDescriptor: PropertyDescriptor): Boolean =
            propertyDescriptor.containingDeclaration is ClassDescriptor &&
            propertyDescriptor.dispatchReceiverParameter == null &&
            propertyDescriptor.extensionReceiverParameter == null

    private fun isPropertyInClass(propertyDescriptor: PropertyDescriptor): Boolean =
            propertyDescriptor.containingDeclaration is ClassDescriptor

    fun getOrCreatePropertyGetter(propertyDescriptor: PropertyDescriptor): PropertyGetterDescriptor =
            propertyGetters.getOrPut(propertyDescriptor) { generateGetter(propertyDescriptor) }

    fun getOrCreatePropertySetter(propertyDescriptor: PropertyDescriptor): PropertySetterDescriptor =
            propertySetters.getOrPut(propertyDescriptor) { generateSetter(propertyDescriptor) }
}
