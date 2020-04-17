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

import org.jetbrains.kotlin.backend.common.DataClassMethodGenerator
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DataClassMembersGenerator
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class DataClassMembersGenerator(
    declarationGenerator: DeclarationGenerator
) : DeclarationGeneratorExtension(declarationGenerator) {

    fun generateInlineClassMembers(ktClassOrObject: KtClassOrObject, irClass: IrClass) {
        MyDataClassMethodGenerator(ktClassOrObject, irClass, IrDeclarationOrigin.GENERATED_INLINE_CLASS_MEMBER).generate()
    }

    fun generateDataClassMembers(ktClassOrObject: KtClassOrObject, irClass: IrClass) {
        MyDataClassMethodGenerator(ktClassOrObject, irClass, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER).generate()
    }

    fun IrMemberAccessExpression.commitSubstituted(descriptor: CallableDescriptor) = context.run { commitSubstituted(descriptor) }

    private fun declareSimpleFunction(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, function: FunctionDescriptor) =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            startOffset, endOffset, origin,
            function
        ).apply {
            returnType = function.returnType!!.toIrType()
        }

    private inner class MyDataClassMethodGenerator(
        ktClassOrObject: KtClassOrObject,
        val irClass: IrClass,
        val origin: IrDeclarationOrigin
    ) : DataClassMethodGenerator(ktClassOrObject, declarationGenerator.context.bindingContext) {

        private val irDataClassMembersGenerator = object : DataClassMembersGenerator(context, context.symbolTable, irClass, origin) {
            override fun declareSimpleFunction(startOffset: Int, endOffset: Int, functionDescriptor: FunctionDescriptor): IrFunction =
                declareSimpleFunction(startOffset, endOffset, origin, functionDescriptor)

            override fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
                FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irFunction)
            }

            override fun getBackingField(parameter: ValueParameterDescriptor?, irValueParameter: IrValueParameter?): IrField? =
                parameter?.let {
                    val property = getOrFail(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter)
                    return getBackingField(property)
                }

            override fun transform(typeParameterDescriptor: TypeParameterDescriptor): IrType =
                typeParameterDescriptor.defaultType.toIrType()

            override fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression, descriptor: CallableDescriptor) {
                irMemberAccessExpression.commitSubstituted(descriptor)
            }
        }

        override fun generateComponentFunction(function: FunctionDescriptor, parameter: ValueParameterDescriptor) {
            if (!irClass.isData) return

            val ktParameter = DescriptorToSourceUtils.descriptorToDeclaration(parameter)
                ?: throw AssertionError("No definition for data class constructor parameter $parameter")

            val backingField = irDataClassMembersGenerator.getBackingField(parameter, null) ?: return
            irDataClassMembersGenerator.generateComponentFunction(
                function, backingField, ktParameter.startOffset, ktParameter.endOffset
            )
        }

        override fun generateCopyFunction(function: FunctionDescriptor, constructorParameters: List<KtParameter>) {
            if (!irClass.isData) return

            val dataClassConstructor = classDescriptor.unsubstitutedPrimaryConstructor
                ?: throw AssertionError("Data class should have a primary constructor: $classDescriptor")
            val constructorSymbol = context.symbolTable.referenceConstructor(dataClassConstructor)

            irDataClassMembersGenerator.generateCopyFunction(function, constructorSymbol)
        }

        override fun generateEqualsMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) =
            irDataClassMembersGenerator.generateEqualsMethod(function, properties)

        override fun generateHashCodeMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) =
            irDataClassMembersGenerator.generateHashCodeMethod(function, properties)

        override fun generateToStringMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) =
            irDataClassMembersGenerator.generateToStringMethod(function, properties)
    }
}
