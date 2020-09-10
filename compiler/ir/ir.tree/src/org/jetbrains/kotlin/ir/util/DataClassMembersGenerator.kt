/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.mapTypeParameters
import org.jetbrains.kotlin.ir.expressions.mapValueParameters
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.representativeUpperBound

/**
 * A platform-, frontend-independent logic for generating synthetic members of data class: equals, hashCode, toString, componentN, and copy.
 * Different front-ends may need to define how to declare functions, parameters, etc., or simply provide predefined ones.
 *
 * Generating synthetic members of inline class can use this as well, in particular, members from Any: equals, hashCode, and toString.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
abstract class DataClassMembersGenerator(
    val context: IrGeneratorContext,
    val symbolTable: SymbolTable,
    val irClass: IrClass,
    val origin: IrDeclarationOrigin
) {

    inline fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            symbolTable.withScope(irDeclaration) {
                builder(irDeclaration)
            }
        }

    private inner class MemberFunctionBuilder(
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        val irFunction: IrFunction
    ) : IrBlockBodyBuilder(context, Scope(irFunction.symbol), startOffset, endOffset) {
        inline fun addToClass(builder: MemberFunctionBuilder.(IrFunction) -> Unit): IrFunction {
            build(builder)
            irClass.declarations.add(irFunction)
            return irFunction
        }

        inline fun build(builder: MemberFunctionBuilder.(IrFunction) -> Unit) {
            irFunction.buildWithScope {
                builder(irFunction)
                irFunction.body = doBuild()
            }
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

        fun putDefault(parameter: ValueParameterDescriptor, value: IrExpression) {
            irFunction.putDefault(parameter, irExprBody(value))
        }

        fun generateComponentFunction(irField: IrField) {
            +irReturn(irGetField(irThis(), irField))
        }

        fun generateCopyFunction(constructorSymbol: IrConstructorSymbol) {
            +irReturn(
                irCall(
                    constructorSymbol,
                    irClass.defaultType,
                    constructedClass = irClass
                ).apply {
                    mapTypeParameters(::transform)
                    mapValueParameters {
                        val irValueParameter = irFunction.valueParameters[it.index]
                        irGet(irValueParameter.type, irValueParameter.symbol)
                    }
                }
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

        private val intTimesSymbol: IrSimpleFunctionSymbol =
            intClass.unsubstitutedMemberScope.findFirstFunction("times") {
                KotlinTypeChecker.DEFAULT.equalTypes(it.valueParameters[0].type, intType)
            }.let { symbolTable.referenceSimpleFunction(it) }

        private val intPlusSymbol: IrSimpleFunctionSymbol =
            intClass.unsubstitutedMemberScope.findFirstFunction("plus") {
                KotlinTypeChecker.DEFAULT.equalTypes(it.valueParameters[0].type, intType)
            }.let { symbolTable.referenceSimpleFunction(it) }

        fun generateHashCodeMethodBody(properties: List<PropertyDescriptor>) {
            if (properties.isEmpty()) {
                +irReturn(irInt(0))
                return
            } else if (properties.size == 1) {
                +irReturn(getHashCodeOfProperty(properties[0]))
                return
            }

            val irIntType = context.irBuiltIns.intType

            val resultVarDescriptor = WrappedVariableDescriptor()
            val irResultVar = IrVariableImpl(
                startOffset, endOffset,
                IrDeclarationOrigin.DEFINED,
                IrVariableSymbolImpl(resultVarDescriptor),
                Name.identifier("result"), irIntType,
                isVar = true, isConst = false, isLateinit = false
            ).also {
                resultVarDescriptor.bind(it)
                it.parent = irFunction
                it.initializer = getHashCodeOfProperty(properties[0])
            }
            +irResultVar

            for (property in properties.drop(1)) {
                val shiftedResult = irCallOp(intTimesSymbol, irIntType, irGet(irResultVar), irInt(31))
                val irRhs = irCallOp(intPlusSymbol, irIntType, shiftedResult, getHashCodeOfProperty(property))
                +irSet(irResultVar.symbol, irRhs)
            }

            +irReturn(irGet(irResultVar))
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

            val hasDispatchReceiver = hashCodeFunctionSymbol.descriptor.dispatchReceiverParameter != null
            return irCall(
                hashCodeFunctionSymbol,
                context.irBuiltIns.intType,
                valueArgumentsCount = if (hasDispatchReceiver) 0 else 1,
                typeArgumentsCount = 0
            ).apply {
                if (hasDispatchReceiver) {
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
        MemberFunctionBuilder(startOffset, endOffset, irFunction).build { function ->
            function.buildWithScope {
                generateSyntheticFunctionParameterDeclarations(function)
                body(function)
            }
        }
    }

    // Entry for psi2ir
    fun generateComponentFunction(function: FunctionDescriptor, irField: IrField, startOffset: Int, endOffset: Int) {
        buildMember(function, startOffset, endOffset) {
            generateComponentFunction(irField)
        }
    }

    // Entry for fir2ir
    fun generateComponentFunction(irFunction: IrFunction, irField: IrField, startOffset: Int, endOffset: Int) {
        buildMember(irFunction, startOffset, endOffset) {
            generateComponentFunction(irField)
        }
    }

    abstract fun getBackingField(parameter: ValueParameterDescriptor?, irValueParameter: IrValueParameter?): IrField?

    abstract fun transform(typeParameterDescriptor: TypeParameterDescriptor): IrType

    // Entry for psi2ir
    fun generateCopyFunction(function: FunctionDescriptor, constructorSymbol: IrConstructorSymbol) {
        buildMember(function, irClass.startOffset, irClass.endOffset) {
            function.valueParameters.forEach { parameter ->
                putDefault(parameter, irGetField(irThis(), getBackingField(parameter, null)!!))
            }
            generateCopyFunction(constructorSymbol)
        }
    }

    // Entry for fir2ir
    fun generateCopyFunction(irFunction: IrFunction, constructorSymbol: IrConstructorSymbol) {
        buildMember(irFunction, irClass.startOffset, irClass.endOffset) {
            irFunction.valueParameters.forEach { irValueParameter ->
                irValueParameter.defaultValue = irExprBody(irGetField(irThis(), getBackingField(null, irValueParameter)!!))
            }
            generateCopyFunction(constructorSymbol)
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

    abstract fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression<*>, descriptor: CallableDescriptor)

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
