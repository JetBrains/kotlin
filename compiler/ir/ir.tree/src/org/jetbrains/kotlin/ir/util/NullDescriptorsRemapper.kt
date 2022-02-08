/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*

object NullDescriptorsRemapper : DescriptorsRemapper {
    override fun remapDeclaredClass(descriptor: ClassDescriptor): ClassDescriptor? = null
    override fun remapDeclaredConstructor(descriptor: ClassConstructorDescriptor): ClassConstructorDescriptor? = null
    override fun remapDeclaredEnumEntry(descriptor: ClassDescriptor): ClassDescriptor? = null
    override fun remapDeclaredField(descriptor: PropertyDescriptor): PropertyDescriptor? = null
    override fun remapDeclaredSimpleFunction(descriptor: FunctionDescriptor): FunctionDescriptor? = null
    override fun remapDeclaredProperty(descriptor: PropertyDescriptor): PropertyDescriptor? = null
    override fun remapDeclaredTypeParameter(descriptor: TypeParameterDescriptor): TypeParameterDescriptor? = null
    override fun remapDeclaredVariable(descriptor: VariableDescriptor): VariableDescriptor? = null
    override fun remapDeclaredValueParameter(descriptor: ParameterDescriptor): ParameterDescriptor? = null
}