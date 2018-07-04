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
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

class DeclarationStubGenerator(
    moduleDescriptor: ModuleDescriptor,
    val symbolTable: SymbolTable,
    val origin: IrDeclarationOrigin
) {
    private val typeTranslator = TypeTranslator(symbolTable)
    private val constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable)

    init {
        typeTranslator.constantValueGenerator = constantValueGenerator
        constantValueGenerator.typeTranslator = typeTranslator
    }

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

    private fun generatePropertyStub(descriptor: PropertyDescriptor): IrProperty =
        IrPropertyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor).also { irProperty ->
            val getterDescriptor = descriptor.getter
            if (getterDescriptor == null) {
                irProperty.backingField =
                        symbolTable.declareField(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            origin,
                            descriptor.original,
                            descriptor.type.toIrType()
                        )
            } else {
                irProperty.getter = generateFunctionStub(getterDescriptor)
            }

            irProperty.setter = descriptor.setter?.let { generateFunctionStub(it) }
        }

    fun generateFunctionStub(descriptor: FunctionDescriptor): IrSimpleFunction =
        symbolTable.declareSimpleFunctionWithOverrides(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
                IrDeclarationOrigin.FAKE_OVERRIDE
            } else {
                origin
            },
            descriptor.original
        ).also { irFunction ->
            generateTypeParameterStubs(descriptor.propertyIfAccessor.typeParameters, irFunction)

            typeTranslator.buildWithScope(irFunction) {
                irFunction.returnType = descriptor.returnType!!.toIrType()
                generateValueParametersStubs(irFunction)
            }
        }

    private fun generateConstructorStub(descriptor: ClassConstructorDescriptor): IrConstructor =
        symbolTable.declareConstructor(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor.original
        ).also { irConstructor ->
            // So far, constructors in Kotlin can't have type parameters of their own.
            irConstructor.returnType = descriptor.returnType.toIrType()
            generateValueParametersStubs(irConstructor)
        }

    private fun generateValueParametersStubs(function: IrFunction) {
        val descriptor = function.descriptor
        function.dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.generateReceiverParameterStub()
        function.extensionReceiverParameter = descriptor.extensionReceiverParameter?.generateReceiverParameterStub()
        descriptor.valueParameters.mapTo(function.valueParameters) { generateValueParameterStub(it) }
    }

    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    private fun ReceiverParameterDescriptor.generateReceiverParameterStub(): IrValueParameter =
        IrValueParameterImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, this,
            type.toIrType(), null
        )

    private fun generateValueParameterStub(descriptor: ValueParameterDescriptor): IrValueParameter =
        IrValueParameterImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
            descriptor, descriptor.type.toIrType(), descriptor.varargElementType?.toIrType()
        ).also { irValueParameter ->
            if (descriptor.declaresDefaultValue()) {
                irValueParameter.defaultValue =
                        IrExpressionBodyImpl(
                            IrErrorExpressionImpl(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET, descriptor.type.toIrType(),
                                "Stub expression for default value of ${descriptor.name}"
                            )
                        )
            }
        }

    private fun generateClassStub(descriptor: ClassDescriptor): IrClass =
        symbolTable.declareClass(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor).also { irClass ->
            generateTypeParameterStubs(descriptor.declaredTypeParameters, irClass)

            typeTranslator.buildWithScope(irClass) {
                // TODO get rid of code duplication, see ClassGenerator#generateClass
                descriptor.typeConstructor.supertypes.mapNotNullTo(irClass.superTypes) {
                    it.toIrType()
                }

                irClass.thisReceiver = descriptor.thisAsReceiverParameter.generateReceiverParameterStub()
                generateChildStubs(descriptor.constructors, irClass)
                generateMemberStubs(descriptor.defaultType.memberScope, irClass)
                generateMemberStubs(descriptor.staticScope, irClass)
            }
        }

    private fun generateEnumEntryStub(descriptor: ClassDescriptor): IrEnumEntry =
        symbolTable.declareEnumEntry(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, descriptor)

    private fun generateTypeParameterStubs(typeParameters: List<TypeParameterDescriptor>, container: IrTypeParametersContainer) {
        typeParameters.mapTo(container.typeParameters) { generateTypeParameterStub(it) }

        typeTranslator.buildWithScope(container) {
            for (typeParameter in container.typeParameters) {
                val descriptor = typeParameter.descriptor
                descriptor.upperBounds.mapTo(typeParameter.superTypes) { it.toIrType() }
            }
        }
    }

    private fun generateTypeParameterStub(descriptor: TypeParameterDescriptor): IrTypeParameter =
        IrTypeParameterImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin,
            descriptor
        )

    private fun generateMemberStubs(memberScope: MemberScope, container: IrDeclarationContainer) {
        generateChildStubs(memberScope.getContributedDescriptors(), container)
    }

    private fun generateChildStubs(descriptors: Collection<DeclarationDescriptor>, container: IrDeclarationContainer) {
        descriptors.mapTo(container.declarations) { generateMemberStub(it) }
    }
}