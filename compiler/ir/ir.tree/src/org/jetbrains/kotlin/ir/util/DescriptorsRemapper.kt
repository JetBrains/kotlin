/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.*

interface DescriptorsRemapper {
    fun remapDeclaredClass(descriptor: ClassDescriptor): ClassDescriptor = descriptor
    fun remapDeclaredScript(descriptor: ScriptDescriptor): ScriptDescriptor = descriptor
    fun remapDeclaredConstructor(descriptor: ClassConstructorDescriptor): ClassConstructorDescriptor = descriptor
    fun remapDeclaredEnumEntry(descriptor: ClassDescriptor): ClassDescriptor = descriptor
    fun remapDeclaredExternalPackageFragment(descriptor: PackageFragmentDescriptor): PackageFragmentDescriptor = descriptor
    fun remapDeclaredField(descriptor: PropertyDescriptor): PropertyDescriptor = descriptor
    fun remapDeclaredFilePackageFragment(descriptor: PackageFragmentDescriptor): PackageFragmentDescriptor = descriptor
    fun remapDeclaredProperty(descriptor: PropertyDescriptor): PropertyDescriptor = descriptor
    fun remapDeclaredSimpleFunction(descriptor: FunctionDescriptor): FunctionDescriptor = descriptor
    fun remapDeclaredTypeParameter(descriptor: TypeParameterDescriptor): TypeParameterDescriptor = descriptor
    fun remapDeclaredValueParameter(descriptor: ParameterDescriptor): ParameterDescriptor = descriptor
    fun remapDeclaredVariable(descriptor: VariableDescriptor): VariableDescriptor = descriptor
    fun remapDeclaredLocalDelegatedProperty(descriptor: VariableDescriptorWithAccessors): VariableDescriptorWithAccessors = descriptor
    fun remapDeclaredTypeAlias(descriptor: TypeAliasDescriptor): TypeAliasDescriptor = descriptor

    object Default : DescriptorsRemapper
}