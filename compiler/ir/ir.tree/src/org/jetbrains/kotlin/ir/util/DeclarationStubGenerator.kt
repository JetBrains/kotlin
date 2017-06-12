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
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.hasDefaultValue
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class DeclarationStubGenerator(
        val symbolTable: SymbolTable,
        val origin: IrDeclarationOrigin
) {
    fun generateEmptyModuleFragmentStub(descriptor: ModuleDescriptor, irBuiltIns: IrBuiltIns): IrModuleFragment =
            IrModuleFragmentImpl(descriptor, irBuiltIns)

    fun generateEmptyExternalPackageFragmentStub(descriptor: PackageFragmentDescriptor): IrExternalPackageFragment =
            symbolTable.declareExternalPackageFragment(descriptor)

    fun generateMemberStub(descriptor: DeclarationDescriptor): IrDeclaration =
            when (descriptor) {
                is ClassDescriptor ->
                    if (DescriptorUtils.isEnumEntry(descriptor))
                        generateEnumEntryStub(descriptor)
                    else
                        generateClassStub(descriptor)
                is ClassConstructorDescriptor ->
                    generateConstructorStub(descriptor)
                is FunctionDescriptor ->
                    generateFunctionStub(descriptor)
                is PropertyDescriptor ->
                    generatePropertyStub(descriptor)
                else ->
                    throw AssertionError("Unexpected member descriptor: $descriptor")
            }

    fun generatePropertyStub(descriptor: PropertyDescriptor): IrProperty =
            IrPropertyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor).also { irProperty ->
                val getterDescriptor = descriptor.getter
                if (getterDescriptor == null) {
                    irProperty.backingField =
                            symbolTable.declareField(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor)
                }
                else {
                    irProperty.getter = generateFunctionStub(getterDescriptor)
                }

                irProperty.setter = descriptor.setter?.let { generateFunctionStub(it) }
            }

    fun generateFunctionStub(descriptor: FunctionDescriptor): IrSimpleFunction =
            symbolTable.declareSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor).also { irFunction ->
                generateTypeParameterStubs(descriptor.typeParameters, irFunction)
                generateValueParametersStubs(descriptor.valueParameters, irFunction)
            }

    fun generateConstructorStub(descriptor: ClassConstructorDescriptor): IrConstructor =
            symbolTable.declareConstructor(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor).also { irConstructor ->
                generateValueParametersStubs(descriptor.valueParameters, irConstructor)
            }

    fun generateValueParametersStubs(valueParameters: Collection<ValueParameterDescriptor>, function: IrFunction) {
        valueParameters.mapTo(function.valueParameters) { generateValueParameterStub(it) }
    }

    fun generateValueParameterStub(descriptor: ValueParameterDescriptor): IrValueParameter =
            IrValueParameterImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor).also { irValueParameter ->
                if (descriptor.declaresDefaultValue()) {
                    irValueParameter.defaultValue =
                            IrExpressionBodyImpl(IrErrorExpressionImpl(
                                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, descriptor.type,
                                    "Stub expression for default value of ${descriptor.name}"
                            ))
                }
            }

    fun generateClassStub(descriptor: ClassDescriptor): IrClass =
            symbolTable.declareClass(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor).also { irClass ->
                generateTypeParameterStubs(descriptor.declaredTypeParameters, irClass)
                generateChildStubs(descriptor.constructors, irClass)
                generateMemberStubs(descriptor.defaultType.memberScope, irClass)
                generateMemberStubs(descriptor.staticScope, irClass)
            }

    fun generateEnumEntryStub(descriptor: ClassDescriptor): IrEnumEntry =
            symbolTable.declareEnumEntry(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor)

    fun generateTypeParameterStubs(typeParameters: List<TypeParameterDescriptor>, container: IrTypeParametersContainer) {
        typeParameters.mapTo(container.typeParameters) { generateTypeParameterStub(it) }
    }

    fun generateTypeParameterStub(descriptor: TypeParameterDescriptor): IrTypeParameter =
            IrTypeParameterImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor)

    fun generateMemberStubs(memberScope: MemberScope, container: IrDeclarationContainer) {
        generateChildStubs(memberScope.getContributedDescriptors(), container)
    }

    fun generateChildStubs(descriptors: Collection<DeclarationDescriptor>, container: IrDeclarationContainer) {
        descriptors.sortedWith(StableDescriptorsComparator).mapTo(container.declarations) { generateMemberStub(it) }
    }
}