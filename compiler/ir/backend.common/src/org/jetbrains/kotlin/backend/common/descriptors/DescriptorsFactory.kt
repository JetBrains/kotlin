/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

interface DescriptorsFactory {
    fun getFieldDescriptorForEnumEntry(enumEntryDescriptor: ClassDescriptor): PropertyDescriptor
    fun getOuterThisFieldDescriptor(classDescriptor: ClassDescriptor): PropertyDescriptor
    fun getInnerClassConstructorWithOuterThisParameter(innerClassConstructor: ClassConstructorDescriptor): ClassConstructorDescriptor
    fun getFieldDescriptorForObjectInstance(objectDescriptor: ClassDescriptor): PropertyDescriptor
}