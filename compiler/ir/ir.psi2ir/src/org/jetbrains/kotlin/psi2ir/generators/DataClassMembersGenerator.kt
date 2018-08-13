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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.putDefault
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.mapTypeParameters
import org.jetbrains.kotlin.ir.expressions.mapValueParameters
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi2ir.containsNull
import org.jetbrains.kotlin.psi2ir.endOffsetOrUndefined
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.startOffsetOrUndefined
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker

class DataClassMembersGenerator(
    declarationGenerator: DeclarationGenerator
) : DeclarationGeneratorExtension(declarationGenerator) {

    fun generateInlineClassMembers(ktClassOrObject: KtClassOrObject, irClass: IrClass) {
        MyDataClassMethodGenerator(ktClassOrObject, irClass, IrDeclarationOrigin.GENERATED_INLINE_CLASS_MEMBER).generate()
    }

    fun generateDataClassMembers(ktClassOrObject: KtClassOrObject, irClass: IrClass) {
        MyDataClassMethodGenerator(ktClassOrObject, irClass, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER).generate()
    }

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

        fun irOther(): IrExpression {
            val irFirstParameter = irFunction.valueParameters[0]
            return IrGetValueImpl(
                startOffset, endOffset,
                irFirstParameter.type,
                irFirstParameter.symbol
            )
        }
    }

    private inner class MyDataClassMethodGenerator(
        ktClassOrObject: KtClassOrObject,
        val irClass: IrClass,
        val origin: IrDeclarationOrigin
    ) : DataClassMethodGenerator(ktClassOrObject, declarationGenerator.context.bindingContext) {
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
                +irReturn(irGet(function.returnType!!.toIrType(), irThis(), getPropertyGetterSymbol(parameter)))
            }
        }

        private fun getPropertyGetterSymbol(parameter: ValueParameterDescriptor): IrFunctionSymbol {
            val property = getOrFail(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter)
            return getPropertyGetterSymbol(property)
        }

        private fun getPropertyGetterSymbol(property: PropertyDescriptor) =
            context.symbolTable.referenceFunction(property.getter!!)

        override fun generateCopyFunction(function: FunctionDescriptor, constructorParameters: List<KtParameter>) {
            if (!irClass.isData) return

            val dataClassConstructor = classDescriptor.unsubstitutedPrimaryConstructor
                ?: throw AssertionError("Data class should have a primary constructor: $classDescriptor")
            val constructorSymbol = context.symbolTable.referenceConstructor(dataClassConstructor)

            buildMember(function, declaration) { irFunction ->
                function.valueParameters.forEach { parameter ->
                    putDefault(parameter, irGet(parameter.type.toIrType(), irThis(), getPropertyGetterSymbol(parameter)))
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

        override fun generateEqualsMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
            buildMember(function, declaration) {
                val irType = classDescriptor.defaultType.toIrType()

                if (!irClass.isInline) {
                    +irIfThenReturnTrue(irEqeqeq(irThis(), irOther()))
                }
                +irIfThenReturnFalse(irNotIs(irOther(), irType))
                val otherWithCast = irTemporary(irAs(irOther(), irType), "other_with_cast")
                for (property in properties) {
                    val irPropertyType = property.type.toIrType()
                    val arg1 = irGet(irPropertyType, irThis(), getPropertyGetterSymbol(property))
                    val arg2 = irGet(irPropertyType, irGet(irType, otherWithCast.symbol), getPropertyGetterSymbol(property))
                    +irIfThenReturnFalse(irNotEquals(arg1, arg2))
                }
                +irReturnTrue()
            }
        }

        private val intClass = context.builtIns.int
        private val intType = context.builtIns.intType

        private val intTimes =
            intClass.findFirstFunction("times") { KotlinTypeChecker.DEFAULT.equalTypes(it.valueParameters[0].type, intType) }
                .let { context.symbolTable.referenceFunction(it) }

        private val intPlus =
            intClass.findFirstFunction("plus") { KotlinTypeChecker.DEFAULT.equalTypes(it.valueParameters[0].type, intType) }
                .let { context.symbolTable.referenceFunction(it) }


        private fun getHashCodeFunction(type: KotlinType): FunctionDescriptor {
            val typeConstructorDescriptor = type.constructor.declarationDescriptor
            return when (typeConstructorDescriptor) {
                is ClassDescriptor ->
                    if (KotlinBuiltIns.isArrayOrPrimitiveArray(typeConstructorDescriptor))
                        context.irBuiltIns.dataClassArrayMemberHashCodeSymbol.descriptor
                    else
                        type.memberScope.findFirstFunction("hashCode") { it.valueParameters.isEmpty() }

                is TypeParameterDescriptor ->
                    getHashCodeFunction(context.builtIns.anyType) // TODO

                else ->
                    throw AssertionError("Unexpected type: $type")
            }

        }

        override fun generateHashCodeMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
            buildMember(function, declaration) {
                val irIntType = context.irBuiltIns.intType
                val result = irTemporaryVar(irInt(0), "result").symbol
                var first = true
                for (property in properties) {
                    val hashCodeOfProperty = getHashCodeOfProperty(irThis(), property)
                    val irNewValue =
                        if (first) hashCodeOfProperty
                        else
                            irCallOp(
                                intPlus,
                                irIntType,
                                irCallOp(
                                    intTimes, irIntType, irGet(irIntType, result), irInt(31)
                                ),
                                hashCodeOfProperty
                            )
                    +irSetVar(result, irNewValue)
                    first = false
                }
                +irReturn(irGet(irIntType, result))
            }
        }

        private fun MemberFunctionBuilder.getHashCodeOfProperty(receiver: IrExpression, property: PropertyDescriptor): IrExpression {
            val getterSymbol = getPropertyGetterSymbol(property)
            val propertyType = property.type
            val irPropertyType = propertyType.toIrType()
            return when {
                propertyType.containsNull() ->
                    irLetS(
                        irGet(irPropertyType, receiver, getterSymbol)
                    ) { variable ->
                        irIfNull(
                            context.irBuiltIns.intType,
                            irGet(irPropertyType, variable),
                            irInt(0),
                            getHashCodeOf(
                                propertyType,
                                irGet(irPropertyType, variable)
                            )
                        )
                    }
                else ->
                    getHashCodeOf(
                        propertyType,
                        irGet(irPropertyType, receiver, getterSymbol)
                    )
            }
        }

        private fun MemberFunctionBuilder.getHashCodeOf(kotlinType: KotlinType, irValue: IrExpression): IrExpression {
            val hashCodeFunctionDescriptor = getHashCodeFunction(kotlinType)
            val hashCodeFunctionSymbol = declarationGenerator.context.symbolTable.referenceFunction(hashCodeFunctionDescriptor.original)
            return irCall(hashCodeFunctionSymbol, hashCodeFunctionDescriptor, context.irBuiltIns.intType).apply {
                if (descriptor.dispatchReceiverParameter != null) {
                    dispatchReceiver = irValue
                } else {
                    putValueArgument(0, irValue)
                }
            }
        }

        override fun generateToStringMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
            buildMember(function, declaration) {
                val irConcat = irConcat()
                irConcat.addArgument(irString(classDescriptor.name.asString() + "("))
                var first = true
                for (property in properties) {
                    val irPropertyType = property.type.toIrType()

                    if (!first) irConcat.addArgument(irString(", "))

                    irConcat.addArgument(irString(property.name.asString() + "="))

                    val irPropertyValue = irGet(irPropertyType, irThis(), getPropertyGetterSymbol(property))

                    val typeConstructorDescriptor = property.type.constructor.declarationDescriptor
                    val irPropertyStringValue =
                        if (typeConstructorDescriptor is ClassDescriptor &&
                            KotlinBuiltIns.isArrayOrPrimitiveArray(typeConstructorDescriptor)
                        )
                            irCall(context.irBuiltIns.dataClassArrayMemberToStringSymbol, context.irBuiltIns.stringType).apply {
                                putValueArgument(0, irPropertyValue)
                            }
                        else
                            irPropertyValue

                    irConcat.addArgument(irPropertyStringValue)
                    first = false
                }
                irConcat.addArgument(irString(")"))
                +irReturn(irConcat)
            }
        }
    }
}
