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
import org.jetbrains.kotlin.ir.expressions.mapValueParameters
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
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
import java.lang.AssertionError

class DataClassMembersGenerator(declarationGenerator: DeclarationGenerator) : DeclarationGeneratorExtension(declarationGenerator) {
    fun generate(ktClassOrObject: KtClassOrObject, irClass: IrClass) {
        MyDataClassMethodGenerator(ktClassOrObject, irClass).generate()
    }

    private fun declareSimpleFunction(startOffset: Int, endOffset: Int, origin: IrDeclarationOrigin, function: FunctionDescriptor) =
        context.symbolTable.declareSimpleFunction(startOffset, endOffset, origin, function)

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

        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, irFunction.dispatchReceiverParameter!!.symbol)

        fun irOther(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, irFunction.valueParameters[0].symbol)
    }

    private inner class MyDataClassMethodGenerator(
        ktClassOrObject: KtClassOrObject,
        val irClass: IrClass
    ) : DataClassMethodGenerator(ktClassOrObject, declarationGenerator.context.bindingContext) {
        private inline fun buildMember(
            function: FunctionDescriptor,
            psiElement: PsiElement? = null,
            body: MemberFunctionBuilder.(IrFunction) -> Unit
        ) {
            MemberFunctionBuilder(
                irClass, function, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER,
                psiElement.startOffsetOrUndefined, psiElement.endOffsetOrUndefined
            ).addToClass { irFunction ->
                irFunction.buildWithScope {
                    FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irFunction)
                    body(irFunction)
                }
            }
        }

        override fun generateComponentFunction(function: FunctionDescriptor, parameter: ValueParameterDescriptor) {
            val ktParameter = DescriptorToSourceUtils.descriptorToDeclaration(parameter)
                    ?: throw AssertionError("No definition for data class constructor parameter $parameter")

            buildMember(function, ktParameter) {
                +irReturn(irGet(irThis(), getPropertyGetterSymbol(parameter)))
            }
        }

        private fun getPropertyGetterSymbol(parameter: ValueParameterDescriptor): IrFunctionSymbol {
            val property = getOrFail(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter)
            return getPropertyGetterSymbol(property)
        }

        private fun getPropertyGetterSymbol(property: PropertyDescriptor) =
            context.symbolTable.referenceFunction(property.getter!!)

        override fun generateCopyFunction(function: FunctionDescriptor, constructorParameters: List<KtParameter>) {
            val dataClassConstructor = classDescriptor.unsubstitutedPrimaryConstructor
                    ?: throw AssertionError("Data class should have a primary constructor: $classDescriptor")
            val constructorSymbol = context.symbolTable.referenceConstructor(dataClassConstructor)

            buildMember(function, declaration) { irFunction ->
                function.valueParameters.forEach { parameter ->
                    putDefault(parameter, irGet(irThis(), getPropertyGetterSymbol(parameter)))
                }
                +irReturn(irCall(constructorSymbol, dataClassConstructor.returnType).mapValueParameters {
                    irGet(irFunction.valueParameters[it.index].symbol)
                })
            }
        }

        override fun generateEqualsMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
            buildMember(function, declaration) {
                +irIfThenReturnTrue(irEqeqeq(irThis(), irOther()))
                +irIfThenReturnFalse(irNotIs(irOther(), classDescriptor.defaultType))
                val otherWithCast = irTemporary(irAs(irOther(), classDescriptor.defaultType), "other_with_cast")
                for (property in properties) {
                    +irIfThenReturnFalse(
                        irNotEquals(
                            irGet(irThis(), getPropertyGetterSymbol(property)),
                            irGet(irGet(otherWithCast.symbol), getPropertyGetterSymbol(property))
                        )
                    )
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


        private fun getHashCodeFunction(type: KotlinType): IrFunctionSymbol {
            val typeConstructorDescriptor = type.constructor.declarationDescriptor
            return when (typeConstructorDescriptor) {
                is ClassDescriptor -> {
                    if (KotlinBuiltIns.isArrayOrPrimitiveArray(typeConstructorDescriptor)) {
                        context.irBuiltIns.dataClassArrayMemberHashCodeSymbol
                    } else {
                        val hashCodeDescriptor: CallableDescriptor =
                            typeConstructorDescriptor.findFirstFunction("hashCode") { it.valueParameters.isEmpty() }
                        context.symbolTable.referenceFunction(hashCodeDescriptor)
                    }
                }
                is TypeParameterDescriptor ->
                    getHashCodeFunction(context.builtIns.anyType) // TODO
                else ->
                    throw AssertionError("Unexpected type: $type")
            }

        }

        override fun generateHashCodeMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
            buildMember(function, declaration) {
                val result = irTemporaryVar(irInt(0), "result").symbol
                var first = true
                for (property in properties) {
                    val hashCodeOfProperty = getHashCodeOfProperty(irThis(), property)
                    val irNewValue =
                        if (first) hashCodeOfProperty
                        else irCallOp(intPlus, irCallOp(intTimes, irGet(result), irInt(31)), hashCodeOfProperty)
                    +irSetVar(result, irNewValue)
                    first = false
                }
                +irReturn(irGet(result))
            }
        }

        private fun MemberFunctionBuilder.getHashCodeOfProperty(receiver: IrExpression, property: PropertyDescriptor): IrExpression {
            val getterSymbol = getPropertyGetterSymbol(property)
            return when {
                property.type.containsNull() ->
                    irLetS(irGet(receiver, getterSymbol)) { variable ->
                        irIfNull(context.builtIns.intType, irGet(variable), irInt(0), getHashCodeOf(irGet(variable)))
                    }
                else ->
                    getHashCodeOf(irGet(receiver, getterSymbol))
            }
        }

        private fun MemberFunctionBuilder.getHashCodeOf(irValue: IrExpression): IrExpression =
            irCall(getHashCodeFunction(irValue.type)).apply {
                if (descriptor.dispatchReceiverParameter != null) {
                    dispatchReceiver = irValue
                } else {
                    putValueArgument(0, irValue)
                }
            }

        override fun generateToStringMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
            buildMember(function, declaration) {
                val irConcat = irConcat()
                irConcat.addArgument(irString(classDescriptor.name.asString() + "("))
                var first = true
                for (property in properties) {
                    if (!first) irConcat.addArgument(irString(", "))
                    irConcat.addArgument(irString(property.name.asString() + "="))
                    val irPropertyValue = irGet(irThis(), getPropertyGetterSymbol(property))
                    val typeConstructorDescriptor = property.type.constructor.declarationDescriptor
                    val irPropertyStringValue =
                        if (typeConstructorDescriptor is ClassDescriptor &&
                            KotlinBuiltIns.isArrayOrPrimitiveArray(typeConstructorDescriptor))
                            irCall(context.irBuiltIns.dataClassArrayMemberToStringSymbol).apply {
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
