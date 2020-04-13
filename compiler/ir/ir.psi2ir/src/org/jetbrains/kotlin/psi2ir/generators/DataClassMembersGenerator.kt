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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.DataClassMethodGenerator
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.mapTypeParameters
import org.jetbrains.kotlin.ir.expressions.mapValueParameters
import org.jetbrains.kotlin.ir.util.DataClassMembersGenerator
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi2ir.endOffsetOrUndefined
import org.jetbrains.kotlin.psi2ir.startOffsetOrUndefined
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

    private inner class MemberFunctionBuilder(
        val irClass: IrClass,
        val function: FunctionDescriptor,
        val origin: IrDeclarationOrigin,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        val irFunction: IrFunction = declareSimpleFunction(startOffset, endOffset, origin, function)
    ) : IrBlockBodyBuilder(context, Scope(irFunction.symbol), startOffset, endOffset) {
        inline fun addToClass(builder: MemberFunctionBuilder.(IrFunction) -> Unit): IrFunction {
            irFunction.buildWithScope {
                builder(irFunction)
                irFunction.body = doBuild()
            }

            irClass.declarations.add(irFunction)
            return irFunction
        }

        fun putDefault(parameter: ValueParameterDescriptor, value: IrExpression) {
            irFunction.putDefault(parameter, irExprBody(value))
        }

        fun irThis(): IrExpression {
            val irDispatchReceiverParameter = irFunction.dispatchReceiverParameter!!
            return IrGetValueImpl(
                startOffset, endOffset,
                irDispatchReceiverParameter.type,
                irDispatchReceiverParameter.symbol
            )
        }
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

            override fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression, descriptor: CallableDescriptor) {
                irMemberAccessExpression.commitSubstituted(descriptor)
            }
        }

        private inline fun buildMember(
            function: FunctionDescriptor,
            psiElement: PsiElement? = null,
            body: MemberFunctionBuilder.(IrFunction) -> Unit
        ) {
            MemberFunctionBuilder(
                irClass, function, origin,
                psiElement.startOffsetOrUndefined, psiElement.endOffsetOrUndefined
            ).addToClass { irFunction ->
                irFunction.buildWithScope {
                    FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irFunction)
                    body(irFunction)
                }
            }
        }

        override fun generateComponentFunction(function: FunctionDescriptor, parameter: ValueParameterDescriptor) {
            if (!irClass.isData) return

            val ktParameter = DescriptorToSourceUtils.descriptorToDeclaration(parameter)
                ?: throw AssertionError("No definition for data class constructor parameter $parameter")

            buildMember(function, ktParameter) {
                +irReturn(irGetField(irThis(), getBackingField(parameter)))
            }
        }

        private fun getBackingField(parameter: ValueParameterDescriptor): IrField {
            val property = getOrFail(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter)
            return irDataClassMembersGenerator.getBackingField(property)
        }

        override fun generateCopyFunction(function: FunctionDescriptor, constructorParameters: List<KtParameter>) {
            if (!irClass.isData) return

            val dataClassConstructor = classDescriptor.unsubstitutedPrimaryConstructor
                ?: throw AssertionError("Data class should have a primary constructor: $classDescriptor")
            val constructorSymbol = context.symbolTable.referenceConstructor(dataClassConstructor)

            buildMember(function, declaration) { irFunction ->
                function.valueParameters.forEach { parameter ->
                    putDefault(parameter, irGetField(irThis(), getBackingField(parameter)))
                }
                +irReturn(
                    irCall(
                        constructorSymbol,
                        dataClassConstructor.returnType.toIrType()
                    ).apply {
                        mapTypeParameters { it.defaultType.toIrType() }
                        mapValueParameters {
                            val irValueParameter = irFunction.valueParameters[it.index]
                            irGet(irValueParameter.type, irValueParameter.symbol)
                        }
                    }
                )
            }
        }

        override fun generateEqualsMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) =
            irDataClassMembersGenerator.generateEqualsMethod(function, properties)

        override fun generateHashCodeMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) =
            irDataClassMembersGenerator.generateHashCodeMethod(function, properties)

        override fun generateToStringMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) =
            irDataClassMembersGenerator.generateToStringMethod(function, properties)
    }
}
