/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.instanceCheckForExternalClass
import org.jetbrains.kotlin.backend.wasm.ir2wasm.getRuntimeClass
import org.jetbrains.kotlin.backend.wasm.ir2wasm.isExternalType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid


class WasmTypeOperatorLowering(val context: WasmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(WasmBaseTypeOperatorTransformer(context))
    }
}

class WasmBaseTypeOperatorTransformer(val context: WasmBackendContext) : IrElementTransformerVoidWithContext() {
    private val symbols = context.wasmSymbols
    private val builtIns = context.irBuiltIns
    private val jsToKotlinAnyAdapter get() = symbols.jsRelatedSymbols.jsInteropAdapters.jsToKotlinAnyAdapter
    private val kotlinToJsAnyAdapter get() = symbols.jsRelatedSymbols.jsInteropAdapters.kotlinToJsAnyAdapter

    private lateinit var builder: DeclarationIrBuilder

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        super.visitTypeOperator(expression)
        builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(expression)

        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_CAST -> lowerImplicitCast(expression)
            IrTypeOperator.IMPLICIT_DYNAMIC_CAST -> error("Dynamic casts are not supported in Wasm backend")
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> expression
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> lowerIntegerCoercion(expression)
            IrTypeOperator.IMPLICIT_NOTNULL -> lowerImplicitCast(expression)
            IrTypeOperator.INSTANCEOF -> lowerInstanceOf(expression, inverted = false)
            IrTypeOperator.NOT_INSTANCEOF -> lowerInstanceOf(expression, inverted = true)
            IrTypeOperator.CAST -> lowerCast(expression, isSafe = false)
            IrTypeOperator.SAFE_CAST -> lowerCast(expression, isSafe = true)
            IrTypeOperator.SAM_CONVERSION -> TODO("SAM conversion: ${expression.render()}")
            IrTypeOperator.REINTERPRET_CAST -> expression
        }
    }

    override fun visitVariable(declaration: IrVariable): IrStatement {
        // Some IR passes, notable for-loops-lowering assumes implicit cast during variable initialization
        val initializer = declaration.initializer
        if (initializer != null &&
            initializer.type != declaration.type
        ) {
            builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(declaration)
            declaration.initializer = narrowType(initializer.type, declaration.type, initializer)
        }

        return super.visitVariable(declaration)
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        if (expression.value.type != expression.symbol.owner.type) {
            builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(expression)
            expression.value = narrowType(expression.value.type, expression.symbol.owner.type, expression.value)
        }

        return super.visitSetValue(expression)
    }

    override fun visitSetField(expression: IrSetField): IrExpression {
        if (expression.value.type != expression.symbol.owner.type) {
            builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(expression)
            expression.value = narrowType(expression.value.type, expression.symbol.owner.type, expression.value)
        }

        return super.visitSetField(expression)
    }

    private fun lowerInstanceOf(
        expression: IrTypeOperatorCall,
        inverted: Boolean
    ): IrExpression {
        return builder.irComposite(resultType = builtIns.booleanType) {
            val argument = cacheValue(expression.argument)
            val check = generateTypeCheck(argument, expression.typeOperand)
            if (inverted) {
                +builder.irNot(check)
            } else {
                +check
            }
        }
    }

    private fun IrBlockBuilder.cacheValue(value: IrExpression): () -> IrExpression {
        if (value.isPure(true) && value.isTrivial()) {
            return { value.deepCopyWithSymbols() }
        }
        val tmpVal = createTmpVariable(value)
        return { builder.irGet(tmpVal) }
    }

    private fun IrType.isInlined(): Boolean =
        context.inlineClassesUtils.isTypeInlined(this)

    private fun generateCCE(valueProvider: () -> IrExpression, fromType: IrType, toType: IrType): IrExpression {
        val klass = toType.erasedUpperBound

        if (klass.isEffectivelyExternal() && klass.isInterface) {
            return builder.irCall(context.symbols.throwTypeCastException)
        }

        val kClass = builder.irCall(context.reflectionSymbols.getKClass).also { getKClassCall ->
            getKClassCall.typeArguments[0] = klass.defaultType
        }

        val value = narrowType(fromType, context.irBuiltIns.anyNType, valueProvider())

        return builder.irCall(context.symbols.throwTypeCastWithInfoException).also { cceCall ->
            cceCall.arguments[0] = value
            cceCall.arguments[1] = kClass
            cceCall.arguments[2] = builder.irBoolean(toType.isNullable())
        }
    }

    private fun generateTypeCheck(
        valueProvider: () -> IrExpression,
        toType: IrType
    ): IrExpression {
        if (toType == context.irBuiltIns.nothingType) {
            return builder.irFalse()
        }

        val toNotNullable = toType.makeNotNull()
        val valueInstance: IrExpression = valueProvider()
        val fromType = valueInstance.type

        // Inlined values have no type information on runtime.
        // But since they are final we can compute type checks on compile time.
        if (fromType.isInlined()) {
            val result = fromType.erasedUpperBound.isSubclassOf(toType.erasedUpperBound)
            return builder.irBoolean(result)
        }

        val instanceCheck = generateTypeCheckNonNull(valueInstance, toNotNullable)
        val isFromNullable = valueInstance.type.isNullable()
        val isToNullable = toType.isNullable()

        return when {
            !isFromNullable -> instanceCheck

            else ->
                builder.irIfThenElse(
                    type = builtIns.booleanType,
                    condition = builder.irEqualsNull(valueProvider()),
                    thenPart = builder.irBoolean(isToNullable),
                    elsePart = instanceCheck
                )
        }
    }

    private fun lowerIntegerCoercion(expression: IrTypeOperatorCall): IrExpression =
        when (expression.typeOperand) {
            builtIns.byteType,
            builtIns.shortType ->
                expression.argument

            builtIns.longType ->
                builder.irCall(symbols.intToLong).apply {
                    arguments[0] = expression.argument
                }

            else -> error("Unreachable execution (coercion to non-Integer type")
        }

    private fun generateTypeCheckNonNull(argument: IrExpression, toType: IrType): IrExpression {
        assert(!toType.isMarkedNullable())
        val classOrInterface = toType.erasedUpperBound
        return when {
            classOrInterface.isExternal -> {
                if (classOrInterface.kind == ClassKind.INTERFACE)
                    builder.irTrue()
                else
                    generateIsExternalClass(argument, classOrInterface)
            }
            toType.isNothing() -> builder.irFalse()
            toType.isTypeParameter() -> generateTypeCheckWithTypeParameter(argument, toType)
            toType.isInterface() -> generateIsInterface(argument, toType)
            else -> generateIsSubClass(argument, toType)
        }
    }

    private fun narrowType(fromType: IrType, toType: IrType, value: IrExpression): IrExpression {
        if (fromType == toType) return value

        if (toType == builtIns.nothingNType) {
            return builder.irComposite(resultType = builtIns.nothingNType) {
                +value
                +builder.irNull()
            }
        }

        // A bit of a hack. Inliner tends to insert null casts from nothing to any. It's hard to express in wasm, so we simply replace
        // them with single const null.
        if (toType == builtIns.anyNType && fromType == builtIns.nothingNType && value is IrConst && value.kind == IrConstKind.Null) {
            return builder.irNull(builtIns.nothingNType)
        }

        // Handled by autoboxing transformer
        if (toType.isInlined() && !fromType.isInlined()) {
            return builder.irCall(
                symbols.unboxIntrinsic,
                toType,
                typeArguments = listOf(fromType, toType)
            ).also {
                it.arguments[0] = value
            }
        }

        if (!toType.isInlined() && fromType.isInlined()) {
            return builder.irCall(
                symbols.boxIntrinsic,
                toType,
                typeArguments = listOf(fromType, toType)
            ).also {
                it.arguments[0] = value
            }
        }

        val fromClass = fromType.erasedUpperBound
        val toClass = toType.erasedUpperBound

        if (fromClass.isExternal && toClass.isExternal) {
            return value
        }

        if (value.isNullConst() && fromClass.isExternal != toClass.isExternal) {
            value.type = toType
            return value
        }

        if (fromClass.isExternal && !toClass.isExternal) {
            if (!context.isWasmJsTarget) {
                TODO("Implement externalize adapter for wasi mode")
            }
            val narrowingToAny = builder.irCall(jsToKotlinAnyAdapter).also {
                it.arguments[0] = value
            }
            // Continue narrowing from Any to expected type
            return narrowType(context.irBuiltIns.anyType, toType, narrowingToAny)
        }

        if (toClass.isExternal && !fromClass.isExternal) {
            if (!context.isWasmJsTarget) {
                TODO("Implement internalize adapter for wasi mode")
            }
            return builder.irCall(kotlinToJsAnyAdapter).also {
                it.arguments[0] = value
            }
        }

        if (fromClass.isSubclassOf(toClass)) {
            return value
        }

        if (toType.isNothing()) {
            // Casting to nothing is unreachable...
            return builder.irComposite(resultType = context.irBuiltIns.nothingType) {
                +value  // ... but we have to evaluate an argument
                +irCall(symbols.wasmUnreachable)
            }
        }

        if (toType == symbols.voidType) {
            return builder.irCall(symbols.findVoidConsumer(value.type)).apply {
                arguments[0] = value
            }
        }

        return builder.irCall(symbols.refCastNull, type = toType).apply {
            typeArguments[0] = toType
            arguments[0] = value
        }
    }

    private fun lowerCast(
        expression: IrTypeOperatorCall,
        isSafe: Boolean,
    ): IrExpression {
        val toType = expression.typeOperand
        val fromType = expression.argument.type
        val expressionType = expression.type

        if (toType != context.irBuiltIns.nothingType &&
            fromType.erasedUpperBound.isSubclassOf(expressionType.erasedUpperBound)
        ) {
            return narrowType(fromType, expressionType, expression.argument)
        }

        return builder.irComposite(resultType = expressionType) {
            val argument = cacheValue(expression.argument)
            val check = generateTypeCheck(argument, toType)
            if (check is IrConst) {
                val value = check.value as Boolean
                if (value) {
                    +narrowType(fromType, expressionType, argument())
                } else {
                    val cceOrNull = if (!isSafe) generateCCE(argument, fromType, toType) else builder.irNull()
                    +cceOrNull
                }
            } else {
                +builder.irIfThenElse(
                    type = expressionType,
                    condition = check,
                    thenPart = narrowType(fromType, expressionType, argument()),
                    elsePart = if (!isSafe) generateCCE(argument, fromType, toType) else builder.irNull()
                )
            }
        }
    }

    private fun shouldGenerateKotlinCast(expression: IrExpression, toType: IrType): Boolean {
        if (toType.isNullableAny()) return false
        if (toType.isTypeParameter()) return false

        val argumentType = when (expression) {
            is IrCall -> {
                val function = expression.symbol.owner

                val packageFragment = function.getPackageFragment()
                if (context.getExcludedPackageFragment(packageFragment.packageFqName) == packageFragment) return false

                function.returnType
            }
            is IrGetField -> expression.symbol.owner.type
            else -> expression.type
        }
        return argumentType.isTypeParameter()
    }

    private fun lowerImplicitCast(expression: IrTypeOperatorCall): IrExpression {
        return if (shouldGenerateKotlinCast(expression.argument, expression.typeOperand)) {
            lowerCast(
                expression = expression,
                isSafe = false
            )
        } else {
            narrowType(
                fromType = expression.argument.type,
                toType = expression.typeOperand,
                value = expression.argument
            )
        }
    }

    private fun generateTypeCheckWithTypeParameter(argument: IrExpression, toType: IrType): IrExpression {
        val typeParameter = toType.classifierOrNull?.owner as? IrTypeParameter
            ?: error("expected type parameter, but got $toType")

        return typeParameter.superTypes.fold(builder.irTrue() as IrExpression) { r, t ->
            val check = generateTypeCheckNonNull(argument.shallowCopy(), t.makeNotNull())
            builder.irCall(symbols.booleanAnd).apply {
                arguments[0] = r
                arguments[1] = check
            }
        }
    }

    private fun generateIsInterface(argument: IrExpression, toType: IrType): IrExpression {
        return builder.irCall(symbols.wasmIsInterface).apply {
            arguments[0] = argument
            typeArguments[0] = toType
        }
    }

    private fun generateIsSubClass(argument: IrExpression, toType: IrType): IrExpression {
        if (toType.isAny()) {
            return builder.irTrue()
        }

        if (toType.isNothing()) {
            return builder.irFalse()
        }

        val fromType = argument.type
        if (isExternalType(fromType) != isExternalType(toType)) {
            return builder.irFalse()
        }

        val fromTypeErased = fromType.getRuntimeClass(context.irBuiltIns)
        val toTypeErased = toType.getRuntimeClass(context.irBuiltIns)
        if (fromTypeErased.isSubclassOf(toTypeErased)) {
            return builder.irTrue()
        }
        if (!toTypeErased.isSubclassOf(fromTypeErased)) {
            return builder.irFalse()
        }

        return builder.irCall(symbols.refTest).apply {
            arguments[0] = argument
            typeArguments[0] = toType
        }
    }

    private fun generateIsExternalClass(argument: IrExpression, klass: IrClass): IrExpression {
        val instanceCheckFunction = klass.instanceCheckForExternalClass!!
        return builder.irCall(instanceCheckFunction).also {
            it.arguments[0] = narrowType(argument.type, context.irBuiltIns.anyType, argument) //TODO("Why we need it?)
        }
    }
}
