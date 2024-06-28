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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.DataClassMembersGenerator
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

/**
 * A generator that generates synthetic members of data class as well as part of inline class.
 *
 * This one uses [DataClassMethodGenerator] to determine which members are needed to generate; uses [DataClassMembersGenerator] to generate
 * function bodies; and provides ways to declare functions or parameters based on descriptors and binding context.
 */
internal class DataClassMembersGenerator(
    declarationGenerator: DeclarationGenerator,
    private val generateBodies: Boolean
) : DeclarationGeneratorExtension(declarationGenerator) {

    fun generateSingleFieldValueClassMembers(ktClassOrObject: KtClassOrObject, irClass: IrClass) {
        MyDataClassMethodGenerator(ktClassOrObject, irClass, IrDeclarationOrigin.GENERATED_SINGLE_FIELD_VALUE_CLASS_MEMBER).generate()
    }

    fun generateMultiFieldValueClassMembers(ktClassOrObject: KtClassOrObject, irClass: IrClass) {
        MyDataClassMethodGenerator(ktClassOrObject, irClass, IrDeclarationOrigin.GENERATED_MULTI_FIELD_VALUE_CLASS_MEMBER).generate()
    }

    fun generateDataClassMembers(ktClassOrObject: KtClassOrObject, irClass: IrClass) {
        MyDataClassMethodGenerator(ktClassOrObject, irClass, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER).generate()
    }

    fun IrMemberAccessExpression<*>.commitSubstituted(descriptor: CallableDescriptor) = context.run { commitSubstituted(descriptor) }

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

        private val irDataClassMembersGenerator = object : DescriptorBasedDataClassMembersGenerator(
            context, context.symbolTable, irClass, ktClassOrObject.fqName, origin,
            forbidDirectFieldAccess = false,
            generateBodies = generateBodies
        ) {
            override fun declareSimpleFunction(startOffset: Int, endOffset: Int, functionDescriptor: FunctionDescriptor): IrFunction =
                declareSimpleFunction(startOffset, endOffset, origin, functionDescriptor)

            override fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
                FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irFunction)
            }

            override fun getProperty(parameter: ValueParameterDescriptor): IrProperty {
                val property = getOrFail(BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameter)
                return getIrProperty(property)
            }

            private fun MemberScope.findHashCodeFunctionOrNull() =
                getContributedFunctions(Name.identifier("hashCode"), NoLookupLocation.FROM_BACKEND)
                    .find { it.valueParameters.isEmpty() && it.extensionReceiverParameter == null }

            private fun getHashCodeFunction(type: KotlinType): FunctionDescriptor =
                type.memberScope.findHashCodeFunctionOrNull()
                    ?: context.irBuiltIns.anyClass.descriptor.unsubstitutedMemberScope.findHashCodeFunctionOrNull()!!

            private fun getHashCodeFunction(
                type: KotlinType,
                symbolResolve: (FunctionDescriptor) -> IrSimpleFunctionSymbol
            ): IrSimpleFunctionSymbol =
                when (val typeConstructorDescriptor = type.constructor.declarationDescriptor) {
                    is ClassDescriptor ->
                        if (KotlinBuiltIns.isArrayOrPrimitiveArray(typeConstructorDescriptor))
                            context.irBuiltIns.dataClassArrayMemberHashCodeSymbol
                        else
                            symbolResolve(getHashCodeFunction(type))

                    is TypeParameterDescriptor ->
                        getHashCodeFunction(typeConstructorDescriptor.representativeUpperBound, symbolResolve)

                    else ->
                        throw AssertionError("Unexpected type: $type")
                }


            inner class Psi2IrHashCodeFunctionInfo(
                override val symbol: IrSimpleFunctionSymbol,
                val substituted: CallableDescriptor
            ) : HashCodeFunctionInfo {

                override fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression<*>) {
                    irMemberAccessExpression.commitSubstituted(substituted)
                }

            }

            override fun getHashCodeFunctionInfo(type: IrType): HashCodeFunctionInfo {
                var substituted: CallableDescriptor? = null
                val symbol = getHashCodeFunction(type.toKotlinType()) { hashCodeDescriptor ->
                    substituted = hashCodeDescriptor
                    symbolTable.descriptorExtension.referenceSimpleFunction(hashCodeDescriptor.original)
                }
                return Psi2IrHashCodeFunctionInfo(symbol, substituted ?: symbol.descriptor)
            }

            override fun IrConstructorSymbol.typesOfTypeParameters(): List<IrType> {
                return descriptor.typeParameters.map { it.defaultType.toIrType() }
            }
        }

        override fun generateComponentFunction(function: FunctionDescriptor, parameter: ValueParameterDescriptor) {
            if (!irClass.isData) return

            val irProperty = irDataClassMembersGenerator.getProperty(parameter)
            irDataClassMembersGenerator.generateComponentFunction(function, irProperty)
        }

        override fun generateCopyFunction(function: FunctionDescriptor, constructorParameters: List<KtParameter>) {
            if (!irClass.isData) return

            val dataClassConstructor = classDescriptor.unsubstitutedPrimaryConstructor
                ?: throw AssertionError("Data class should have a primary constructor: $classDescriptor")
            val constructorSymbol = context.symbolTable.descriptorExtension.referenceConstructor(dataClassConstructor)

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

@OptIn(ObsoleteDescriptorBasedAPI::class)
private abstract class DescriptorBasedDataClassMembersGenerator(
    context: IrGeneratorContext,
    symbolTable: ReferenceSymbolTable,
    irClass: IrClass,
    fqName: FqName?,
    origin: IrDeclarationOrigin,
    forbidDirectFieldAccess: Boolean,
    val generateBodies: Boolean
) : DataClassMembersGenerator(context, symbolTable, irClass, fqName, origin, forbidDirectFieldAccess) {
    private val irPropertiesByDescriptor: Map<PropertyDescriptor, IrProperty> =
        irClass.properties.associateBy { it.descriptor }

    fun generateEqualsMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
        buildMember(function) {
            generateEqualsMethodBody(properties.map { getIrProperty(it) })
        }
    }

    fun generateComponentFunction(function: FunctionDescriptor, irProperty: IrProperty) {
        buildMember(function) {
            generateComponentFunction(irProperty)
        }
    }

    fun generateCopyFunction(function: FunctionDescriptor, constructorSymbol: IrConstructorSymbol) {
        buildMember(function) {
            if (generateBodies) {
                function.valueParameters.forEach { parameter ->
                    putDefault(parameter, irGetProperty(irThis(), getProperty(parameter)))
                }
                generateCopyFunction(constructorSymbol)
            }
        }
    }

    fun generateHashCodeMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
        buildMember(function) {
            generateHashCodeMethodBody(
                properties.map { getIrProperty(it) },
                if (irClass.kind == ClassKind.OBJECT && irClass.isData) fqName.hashCode() else 0
            )
        }
    }

    fun generateToStringMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
        buildMember(function) {
            generateToStringMethodBody(properties.map { getIrProperty(it) })
        }
    }

    fun getIrProperty(property: PropertyDescriptor): IrProperty {
        return irPropertiesByDescriptor[property]
            ?: error("Class: ${irClass.descriptor}: unexpected property descriptor: $property")
    }


    abstract fun declareSimpleFunction(startOffset: Int, endOffset: Int, functionDescriptor: FunctionDescriptor): IrFunction
    abstract fun getProperty(parameter: ValueParameterDescriptor): IrProperty

    // Build a member from a descriptor (psi2ir) as well as its body.
    private inline fun buildMember(
        function: FunctionDescriptor,
        startOffset: Int = SYNTHETIC_OFFSET,
        endOffset: Int = SYNTHETIC_OFFSET,
        body: MemberFunctionBuilder.(IrFunction) -> Unit
    ) {
        MemberFunctionBuilder(startOffset, endOffset, declareSimpleFunction(startOffset, endOffset, function)).addToClass { irFunction ->
            irFunction.buildWithScope {
                irFunction.parent = irClass
                generateSyntheticFunctionParameterDeclarations(irFunction)
                body(irFunction)
            }
        }
    }

    private fun MemberFunctionBuilder.putDefault(parameter: ValueParameterDescriptor, value: IrExpression) {
        irFunction.putDefault(parameter, irExprBody(value))
    }

    override fun IrSimpleFunctionSymbol.hasDispatchReceiver(): Boolean {
        return descriptor.dispatchReceiverParameter != null
    }
}
