/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irIfThenReturnFalse
import org.jetbrains.kotlin.ir.builders.irIfThenReturnTrue
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNotIs
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irReturnTrue
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

abstract class DataClassMembersGenerator(
    val context: IrGeneratorContext,
    val symbolTable: SymbolTable,
    val irClass: IrClass,
    val origin: IrDeclarationOrigin
) {

    inline fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            symbolTable.withScope(irDeclaration.descriptor) {
                builder(irDeclaration)
            }
        }

    private inner class MemberFunctionBuilder(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        val irFunction: IrFunction
    ) : IrBlockBodyBuilder(context, Scope(irFunction.symbol), startOffset, endOffset) {
        inline fun addToClass(builder: MemberFunctionBuilder.(IrFunction) -> Unit): IrFunction {
            irFunction.buildWithScope {
                builder(irFunction)
                irFunction.body = doBuild()
            }

            irClass.declarations.add(irFunction)
            return irFunction
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

        fun generateEqualsMethodBody(properties: List<PropertyDescriptor>) {
            val irType = irClass.defaultType

            if (!irClass.isInline) {
                +irIfThenReturnTrue(irEqeqeq(irThis(), irOther()))
            }
            +irIfThenReturnFalse(irNotIs(irOther(), irType))
            val otherWithCast = irTemporary(irAs(irOther(), irType), "other_with_cast")
            for (property in properties) {
                val field = getBackingField(property)
                val arg1 = irGetField(irThis(), field)
                val arg2 = irGetField(irGet(irType, otherWithCast.symbol), field)
                +irIfThenReturnFalse(irNotEquals(arg1, arg2))
            }
            +irReturnTrue()
        }

        private val intClass = context.builtIns.int
        private val intType = context.builtIns.intType

        private val intTimesSymbol: IrFunctionSymbol =
            intClass.findFirstFunction("times") { KotlinTypeChecker.DEFAULT.equalTypes(it.valueParameters[0].type, intType) }
                .let { symbolTable.referenceFunction(it) }

        private val intPlusSymbol: IrFunctionSymbol =
            intClass.findFirstFunction("plus") { KotlinTypeChecker.DEFAULT.equalTypes(it.valueParameters[0].type, intType) }
                .let { symbolTable.referenceFunction(it) }

        fun generateHashCodeMethodBody(properties: List<PropertyDescriptor>) {
            val irIntType = context.irBuiltIns.intType
            var result: IrExpression? = null
            for (property in properties) {
                val hashCodeOfProperty = getHashCodeOfProperty(property)
                result = if (result == null) {
                    hashCodeOfProperty
                } else {
                    val shiftedResult = irCallOp(intTimesSymbol, irIntType, result, irInt(31))
                    irCallOp(intPlusSymbol, irIntType, shiftedResult, hashCodeOfProperty)
                }
            }
            +irReturn(result ?: irInt(0))
        }

        private fun getHashCodeOfProperty(property: PropertyDescriptor): IrExpression {
            val field = getBackingField(property)
            return when {
                property.type.isNullable() ->
                    irIfNull(
                        context.irBuiltIns.intType,
                        irGetField(irThis(), field),
                        irInt(0),
                        getHashCodeOf(property, irGetField(irThis(), field))
                    )
                else ->
                    getHashCodeOf(property, irGetField(irThis(), field))
            }
        }

        private fun getHashCodeOf(property: PropertyDescriptor, irValue: IrExpression): IrExpression {
            var substituted: FunctionDescriptor? = null
            val hashCodeFunctionSymbol = getHashCodeFunction(property) {
                substituted = it
                symbolTable.referenceSimpleFunction(it.original)
            }

            return irCall(hashCodeFunctionSymbol, context.irBuiltIns.intType).apply {
                if (hashCodeFunctionSymbol.descriptor.dispatchReceiverParameter != null) {
                    dispatchReceiver = irValue
                } else {
                    putValueArgument(0, irValue)
                }
                commitSubstituted(this, substituted ?: hashCodeFunctionSymbol.descriptor)
            }
        }

        fun generateToStringMethodBody(properties: List<PropertyDescriptor>) {
            val irConcat = irConcat()
            irConcat.addArgument(irString(irClass.descriptor.name.asString() + "("))
            var first = true
            for (property in properties) {
                if (!first) irConcat.addArgument(irString(", "))

                irConcat.addArgument(irString(property.name.asString() + "="))

                val irPropertyValue = irGetField(irThis(), getBackingField(property))

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

    fun getBackingField(property: PropertyDescriptor): IrField =
        irClass.properties.single { it.descriptor == property }.backingField!!

    abstract fun declareSimpleFunction(startOffset: Int, endOffset: Int, functionDescriptor: FunctionDescriptor): IrFunction

    abstract fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction)

    // Build a member from a descriptor (psi2ir) as well as its body.
    private inline fun buildMember(
        function: FunctionDescriptor,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        body: MemberFunctionBuilder.(IrFunction) -> Unit
    ) {
        MemberFunctionBuilder(startOffset, endOffset, declareSimpleFunction(startOffset, endOffset, function)).addToClass { irFunction ->
            irFunction.buildWithScope {
                generateSyntheticFunctionParameterDeclarations(irFunction)
                body(irFunction)
            }
        }
    }

    // Use a prebuilt member (fir2ir) and build a member body for it.
    private inline fun buildMember(
        irFunction: IrFunction,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        body: MemberFunctionBuilder.(IrFunction) -> Unit
    ) {
        MemberFunctionBuilder(startOffset, endOffset, irFunction).addToClass { function ->
            function.buildWithScope {
                generateSyntheticFunctionParameterDeclarations(function)
                body(function)
            }
        }
    }

   // Entry for psi2ir
    fun generateEqualsMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
        buildMember(function, irClass.startOffset, irClass.endOffset) {
            generateEqualsMethodBody(properties)
        }
    }

    // Entry for fir2ir
    fun generateEqualsMethod(irFunction: IrFunction, properties: List<PropertyDescriptor>) {
        buildMember(irFunction, irClass.startOffset, irClass.endOffset) {
            generateEqualsMethodBody(properties)
        }
    }

    private fun MemberScope.findHashCodeFunctionOrNull() =
        getContributedFunctions(Name.identifier("hashCode"), NoLookupLocation.FROM_BACKEND)
            .find { it.valueParameters.isEmpty() }

    private fun getHashCodeFunction(type: KotlinType): FunctionDescriptor =
        type.memberScope.findHashCodeFunctionOrNull()
            ?: context.builtIns.any.unsubstitutedMemberScope.findHashCodeFunctionOrNull()!!

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

    private fun getHashCodeFunction(
        property: PropertyDescriptor,
        symbolResolve: (FunctionDescriptor) -> IrSimpleFunctionSymbol
    ): IrSimpleFunctionSymbol =
        getHashCodeFunction(property.type, symbolResolve)

    abstract fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression, descriptor: CallableDescriptor)

    // Entry for psi2ir
    fun generateHashCodeMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
        buildMember(function, irClass.startOffset, irClass.endOffset) {
            generateHashCodeMethodBody(properties)
        }
    }

    // Entry for fir2ir
    fun generateHashCodeMethod(irFunction: IrFunction, properties: List<PropertyDescriptor>) {
        buildMember(irFunction, irClass.startOffset, irClass.endOffset) {
            generateHashCodeMethodBody(properties)
        }
    }

    // Entry for psi2ir
    fun generateToStringMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
        buildMember(function, irClass.startOffset, irClass.endOffset) {
            generateToStringMethodBody(properties)
        }
    }

    // Entry for fir2ir
    fun generateToStringMethod(irFunction: IrFunction, properties: List<PropertyDescriptor>) {
        buildMember(irFunction, irClass.startOffset, irClass.endOffset) {
            generateToStringMethodBody(properties)
        }
    }
}

