/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.putArgument
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * A platform-, frontend-independent logic for generating synthetic members of data class: equals, hashCode, toString, componentN, and copy.
 * Different front-ends may need to define how to declare functions, parameters, etc., or simply provide predefined ones.
 *
 * Generating synthetic members of inline class can use this as well, in particular, members from Any: equals, hashCode, and toString.
 */
abstract class DataClassMembersGenerator(
    val context: IrGeneratorContext,
    val symbolTable: ReferenceSymbolTable,
    val irClass: IrClass,
    val fqName: FqName?,
    val origin: IrDeclarationOrigin,
    val forbidDirectFieldAccess: Boolean = false,
) {
    inline fun <T : IrDeclaration> T.buildWithScope(builder: (T) -> Unit): T =
        also { irDeclaration ->
            symbolTable.withReferenceScope(irDeclaration) {
                builder(irDeclaration)
            }
        }

    protected val IrProperty.type
        get() = this.backingField?.type ?: this.getter?.returnType ?: error("Can't find type of ${this.render()}")

    protected inner class MemberFunctionBuilder(
        startOffset: Int = SYNTHETIC_OFFSET,
        endOffset: Int = SYNTHETIC_OFFSET,
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

        private fun irOther(): IrExpression {
            val irFirstParameter = irFunction.valueParameters[0]
            return IrGetValueImpl(
                startOffset, endOffset,
                irFirstParameter.type,
                irFirstParameter.symbol
            )
        }

        fun irGetProperty(receiver: IrExpression, property: IrProperty): IrExpression {
            // In some JVM-specific cases, such as when 'allopen' compiler plugin is applied,
            // data classes and corresponding properties can be non-final.
            // We should use getters for such properties (see KT-41284).
            val backingField = property.backingField
            return if (!forbidDirectFieldAccess && irClass.isFinalClass && backingField != null) {
                irGetField(receiver, backingField)
            } else {
                irCall(property.getter!!).apply {
                    dispatchReceiver = receiver
                }
            }
        }

        fun generateComponentFunction(irProperty: IrProperty) {
            +irReturn(irGetProperty(irThis(), irProperty))
        }

        fun generateCopyFunction(constructorSymbol: IrConstructorSymbol) {
            +irReturn(
                irCall(
                    constructorSymbol,
                    irClass.defaultType,
                    constructedClass = irClass
                ).apply {
                    for ((i, typeParameterType) in constructorSymbol.typesOfTypeParameters().withIndex()) {
                        putTypeArgument(i, typeParameterType)
                    }
                    for ((i, valueParameter) in irFunction.valueParameters.withIndex()) {
                        putValueArgument(i, irGet(valueParameter.type, valueParameter.symbol))
                    }
                }
            )
        }

        private fun IrSimpleFunction.isTypedEqualsInValueClass() = name == OperatorNameConventions.EQUALS &&
                returnType == context.irBuiltIns.booleanType && irClass.isValue
                && valueParameters.size == 1 && valueParameters[0].type.classifierOrNull == irClass.symbol
                && contextReceiverParametersCount == 0 && extensionReceiverParameter == null

        fun generateEqualsMethodBody(properties: List<IrProperty>) {
            val irType = irClass.defaultType

            val typedEqualsFunction = irClass.functions.singleOrNull { it.isTypedEqualsInValueClass() }
            if (irClass.isValue && typedEqualsFunction != null) {
                +irIfThenReturnFalse(irNotIs(irOther(), irType))
                val otherCasted = irImplicitCast(irOther(), irType)
                +irReturn(irCall(typedEqualsFunction).apply {
                    putArgument(typedEqualsFunction.dispatchReceiverParameter!!, irThis())
                    putValueArgument(0, otherCasted)
                })
                return
            }

            if (!irClass.isValue) {
                +irIfThenReturnTrue(irEqeqeq(irThis(), irOther()))
            }
            +irIfThenReturnFalse(irNotIs(irOther(), irType))
            val otherWithCast = irTemporary(irAs(irOther(), irType), "other_with_cast")
            for (property in properties) {
                val arg1 = irGetProperty(irThis(), property)
                val arg2 = irGetProperty(irGet(irType, otherWithCast.symbol), property)
                +irIfThenReturnFalse(irNotEquals(arg1, arg2))
            }
            +irReturnTrue()
        }

        fun generateHashCodeMethodBody(properties: List<IrProperty>, constHashCode: Int) {
            if (properties.isEmpty()) {
                +irReturn(irInt(constHashCode))
                return
            } else if (properties.size == 1) {
                +irReturn(getHashCodeOfProperty(properties[0]))
                return
            }

            val irIntType = context.irBuiltIns.intType

            val irResultVar = IrVariableImpl(
                startOffset, endOffset,
                IrDeclarationOrigin.DEFINED,
                IrVariableSymbolImpl(),
                Name.identifier("result"), irIntType,
                isVar = true, isConst = false, isLateinit = false
            ).also {
                it.parent = irFunction
                it.initializer = getHashCodeOfProperty(properties[0])
            }
            +irResultVar

            for (property in properties.drop(1)) {
                val shiftedResult = shiftResultOfHashCode(irResultVar)
                val irRhs = irCallOp(context.irBuiltIns.intPlusSymbol, irIntType, shiftedResult, getHashCodeOfProperty(property))
                +irSet(irResultVar.symbol, irRhs)
            }

            +irReturn(irGet(irResultVar))
        }

        private fun getHashCodeOfProperty(property: IrProperty): IrExpression {
            return when {
                property.type.isNullable() ->
                    irIfNull(
                        context.irBuiltIns.intType,
                        irGetProperty(irThis(), property),
                        irInt(0),
                        getHashCodeOf(this, property, irGetProperty(irThis(), property))
                    )
                else ->
                    getHashCodeOf(this, property, irGetProperty(irThis(), property))
            }
        }

        fun generateToStringMethodBody(properties: List<IrProperty>) {
            if (properties.isEmpty() && irClass.kind == ClassKind.OBJECT) {
                +irReturn(irString(irClass.name.asString()))
                return
            }
            val irConcat = irConcat()
            irConcat.addArgument(irString(irClass.classNameForToString() + "("))
            var first = true
            for (property in properties) {
                if (!first) irConcat.addArgument(irString(", "))

                irConcat.addArgument(irString(property.name.asString() + "="))

                val irPropertyValue = irGetProperty(irThis(), property)

                val classifier = property.type.classifierOrNull
                val irPropertyStringValue =
                    if (classifier.isArrayOrPrimitiveArray)
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

    protected open fun IrBuilderWithScope.shiftResultOfHashCode(irResultVar: IrVariable): IrExpression =
        irCallOp(context.irBuiltIns.intTimesSymbol, context.irBuiltIns.intType, irGet(irResultVar), irInt(31))

    protected open fun getHashCodeOf(builder: IrBuilderWithScope, property: IrProperty, irValue: IrExpression): IrExpression {
        return builder.getHashCodeOf(getHashCodeFunctionInfo(property), irValue)
    }

    protected fun IrBuilderWithScope.getHashCodeOf(hashCodeFunctionInfo: HashCodeFunctionInfo, irValue: IrExpression): IrExpression {
        val hashCodeFunctionSymbol = hashCodeFunctionInfo.symbol
        val hasDispatchReceiver = hashCodeFunctionSymbol.hasDispatchReceiver()
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
            hashCodeFunctionInfo.commitSubstituted(this)
        }
    }

    val IrClassifierSymbol?.isArrayOrPrimitiveArray: Boolean
        get() = isArrayOrPrimitiveArray(context.irBuiltIns)

    abstract fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction)

    interface HashCodeFunctionInfo {
        val symbol: IrSimpleFunctionSymbol
        fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression<*>)
    }

    open fun getHashCodeFunctionInfo(property: IrProperty): HashCodeFunctionInfo {
        return getHashCodeFunctionInfo(property.type)
    }

    abstract fun getHashCodeFunctionInfo(type: IrType): HashCodeFunctionInfo

    open fun IrClass.classNameForToString(): String = irClass.name.asString()

    protected abstract fun IrSimpleFunctionSymbol.hasDispatchReceiver(): Boolean
    protected abstract fun IrConstructorSymbol.typesOfTypeParameters(): List<IrType>
}

abstract class IrBasedDataClassMembersGenerator(
    context: IrGeneratorContext,
    symbolTable: ReferenceSymbolTable,
    irClass: IrClass,
    fqName: FqName?,
    origin: IrDeclarationOrigin,
    forbidDirectFieldAccess: Boolean,
) : DataClassMembersGenerator(context, symbolTable, irClass, fqName, origin, forbidDirectFieldAccess) {
    fun generateComponentFunction(irFunction: IrFunction, irProperty: IrProperty) {
        buildMember(irFunction) {
            generateComponentFunction(irProperty)
        }
    }

    fun generateCopyFunction(irFunction: IrFunction, constructorSymbol: IrConstructorSymbol) {
        buildMember(irFunction) {
            irFunction.valueParameters.forEach { irValueParameter ->
                irValueParameter.defaultValue = irExprBody(irGetProperty(irThis(), getProperty(irValueParameter)))
            }
            generateCopyFunction(constructorSymbol)
        }
    }

    fun generateEqualsMethod(irFunction: IrFunction, properties: List<IrProperty>) {
        buildMember(irFunction) {
            generateEqualsMethodBody(properties)
        }
    }

    fun generateHashCodeMethod(irFunction: IrFunction, properties: List<IrProperty>) {
        buildMember(irFunction) {
            generateHashCodeMethodBody(
                properties,
                if (irClass.kind == ClassKind.OBJECT && irClass.isData) fqName.hashCode() else 0
            )
        }
    }

    fun generateToStringMethod(irFunction: IrFunction, properties: List<IrProperty>) {
        buildMember(irFunction) {
            generateToStringMethodBody(properties)
        }
    }

    // Use a prebuilt member and build a member body for it.
    private inline fun buildMember(
        irFunction: IrFunction,
        startOffset: Int = SYNTHETIC_OFFSET,
        endOffset: Int = SYNTHETIC_OFFSET,
        body: MemberFunctionBuilder.(IrFunction) -> Unit
    ) {
        MemberFunctionBuilder(startOffset, endOffset, irFunction).build { function ->
            function.buildWithScope {
                generateSyntheticFunctionParameterDeclarations(function)
                body(function)
            }
        }
    }

    abstract fun getProperty(irValueParameter: IrValueParameter?): IrProperty

    override fun IrSimpleFunctionSymbol.hasDispatchReceiver(): Boolean {
        return owner.dispatchReceiverParameter != null
    }

    override fun IrConstructorSymbol.typesOfTypeParameters(): List<IrType> {
        val allParameters = owner.constructedClass.typeParameters + owner.typeParameters
        return allParameters.map { it.defaultType }
    }
}
