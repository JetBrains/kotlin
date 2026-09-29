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
import org.jetbrains.kotlin.backend.wasm.jsFunctionForExternalAdapterFunction
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.findUnitGetInstanceFunction
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.backend.js.ir.JsIrBuilder
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOf
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget


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
    private val unitGetInstance by lazy { context.findUnitGetInstanceFunction() }

    private lateinit var builder: DeclarationIrBuilder

    // Functions of the enclosing inlined function blocks (null for inlined lambdas).
    private val enclosingInlinedFunctions = ArrayDeque<IrFunction?>()

    // The implicit cast which initializes the compiler temporary currently being visited, if any.
    private var temporaryInitializer: IrTypeOperatorCall? = null

    override fun visitInlinedFunctionBlock(inlinedBlock: IrInlinedFunctionBlock): IrExpression {
        enclosingInlinedFunctions.addLast(inlinedBlock.inlinedFunctionSymbol?.owner)
        try {
            return super.visitInlinedFunctionBlock(inlinedBlock)
        } finally {
            enclosingInlinedFunctions.removeLast()
        }
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        // Must be decided before the argument is lowered, as that changes its shape and type.
        val isImplicit = expression.operator == IrTypeOperator.IMPLICIT_CAST || expression.operator == IrTypeOperator.IMPLICIT_NOTNULL
        val needsRuntimeCheck = isImplicit && shouldGenerateKotlinCast(expression)
        super.visitTypeOperator(expression)
        builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(expression)

        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_CAST -> lowerImplicitCast(expression, needsRuntimeCheck)
            IrTypeOperator.IMPLICIT_DYNAMIC_CAST -> error("Dynamic casts are not supported in Wasm backend")
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> expression
            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> lowerIntegerCoercion(expression)
            IrTypeOperator.IMPLICIT_NOTNULL -> lowerImplicitCast(expression, needsRuntimeCheck)
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
        temporaryInitializer = (initializer as? IrTypeOperatorCall)?.takeIf { declaration.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE }
        if (initializer != null &&
            initializer.type != declaration.type
        ) {
            builder = context.createIrBuilder(currentScope!!.scope.scopeOwnerSymbol).at(declaration)
            declaration.initializer = narrowType(initializer.type, declaration.type, initializer)
        }

        return super.visitVariable(declaration)
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

        // For functional arguments which are declared to return the
        // Unit type, we still need to accept functions declared as
        // e.g. fun <T> List<T>.foo(): T, but making sure to ignore
        // the result to return Unit instead. We essentially emulate
        // what is done for IMPLICIT_COERCION_TO_UNIT by the body
        // generator.
        if (toType.isUnit()) {
            return builder.irComposite(resultType = builtIns.unitType) {
                +value
                +builder.irCall(unitGetInstance)
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
            return builder.irCall(symbols.consumeAnyIntoVoid).apply {
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

    /**
     * Whether an implicit cast narrows a value produced at an erased generic type back to a substituted type (an erasure
     * boundary), where heap pollution caused by an unchecked cast has to throw a ClassCastException. The IR doesn't record
     * this, so it's reconstructed from the shapes which the preceding lowerings leave behind.
     */
    private fun shouldGenerateKotlinCast(cast: IrTypeOperatorCall): Boolean {
        val toType = cast.typeOperand
        if (toType.isNullableAny()) return false
        if (toType.isTypeParameter()) return false
        // For the cases of casts of callable references to return Unit type, such as
        //
        // fun suspect(): Dummy { ... }
        // ::suspect as () -> Unit
        //
        // just need to return a Unit instance +builder.irCall(unitGetInstance)
        // instead of trying to cast to Unit
        //
        // also fixes testData/codegen/box/basics/unchecked_cast10.kt
        if (toType.isUnit()) return false

        // Look through the chain of implicit casts on top of the value, e.g. the inliner upcasts the result of an inline function
        // to the erased return type before narrowing it to the substituted one (unchecked_cast13.kt).
        var argument = cast.argument
        while (true) {
            // A value still statically typed as a type parameter comes straight from generic code.
            if (argument.type.isTypeParameter()) return true
            argument = (argument as? IrTypeOperatorCall)?.takeIf { it.operator == IrTypeOperator.IMPLICIT_CAST }?.argument ?: break
        }
        return when (argument) {
            // The result of a call to a function returning a type parameter. Looks at the real override target, since a fake
            // override declares the substituted return type (KT-88828). Intrinsics excluded from codegen (`unsafeCast` and
            // friends) are meant to narrow without a check.
            is IrCall -> !argument.symbol.owner.isExcludedFromCodegen() &&
                    argument.symbol.owner.realOverrideTarget.returnType.isTypeParameter()
            // A read of a generic field.
            is IrGetField -> argument.symbol.owner.type.isTypeParameter()
            // The result of an inline function returning a type parameter, which the inliner erased (unchecked_cast13.kt).
            is IrReturnableBlock, is IrInlinedFunctionBlock -> argument.inlinedFunction()?.returnsErasedTypeParameter() == true
            is IrGetValue -> when (val value = argument.symbol.owner) {
                is IrVariable -> value.isSuspendResult() || (cast.isInlinerCastOfErasedValue() && !value.isProvenToBe(toType))
                else -> false
            }
            else -> false
        }
    }

    private fun IrExpression.inlinedFunction(): IrFunction? = when (this) {
        is IrInlinedFunctionBlock -> inlinedFunctionSymbol?.owner
        is IrReturnableBlock -> (statements.singleOrNull() as? IrInlinedFunctionBlock)?.inlinedFunctionSymbol?.owner
        else -> null
    }

    private fun IrFunction.returnsErasedTypeParameter(): Boolean =
        (returnType.classifierOrNull?.owner as? IrTypeParameter)?.isReified == false

    private fun IrSimpleFunction.isExcludedFromCodegen(): Boolean {
        val packageFragment = getPackageFragment()
        return context.getExcludedPackageFragment(packageFragment.packageFqName) == packageFragment
    }

    // The coroutine state machine reads the result of every suspend call back from this `Any?` variable. Whether the callee
    // returned a type parameter is lost by then, so all of these reads are treated as erasure boundaries.
    private fun IrVariable.isSuspendResult(): Boolean =
        name.asString() == "suspendResult" && origin == JsIrBuilder.SYNTHESIZED_DECLARATION

    // The inliner binds a value produced by the body of an inline function whose type parameters it erased (e.g. an element
    // in `forEach`) to a parameter of an inlined lambda through a temporary initialized with an implicit cast (KT-87090).
    // This also matches smart casts which the inliner re-applies, which then get a redundant check.
    private fun IrTypeOperatorCall.isInlinerCastOfErasedValue(): Boolean =
        this === temporaryInitializer &&
                enclosingInlinedFunctions.any { function -> function != null && function.typeParameters.any { !it.isReified } }

    // The inliner stores the arguments of an inline function in temporaries typed with the erased parameter type. If the value
    // stored had the target type already, e.g. the receiver of `arrayOfNulls(n).also { cache = it }`, the cast is proven.
    // Each implicit cast in the initializer is decided on its own, so any of their types can be relied upon. The value may be
    // copied through several such temporaries (e.g. the inliner's argument temporary, then the inline function's `this`).
    // Nullability is ignored: the inliner unwraps smart casts (e.g. from a safe call), and like a JVM checkcast, the checks at
    // the inliner's erasure boundaries let null through anyway.
    private fun IrVariable.isProvenToBe(type: IrType): Boolean {
        if (isVar) return false
        val nullableType = type.makeNullable()
        var value: IrExpression? = initializer
        while (value != null) {
            if (value.type.isSubtypeOf(nullableType, context.typeSystem)) return true
            value = when (value) {
                is IrTypeOperatorCall -> value.argument.takeIf { value.operator == IrTypeOperator.IMPLICIT_CAST }
                is IrGetValue -> (value.symbol.owner as? IrVariable)?.takeIf { !it.isVar }?.initializer
                else -> null
            }
        }
        return false
    }

    private fun lowerImplicitCast(expression: IrTypeOperatorCall, needsRuntimeCheck: Boolean): IrExpression {
        return if (needsRuntimeCheck) {
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
            if (fromType.classifierOrNull == symbols.jsRelatedSymbols.jsReferenceClass ||
                fromType.classifierOrNull == symbols.jsRelatedSymbols.jsAnyClass
            ) {
                // special case: JsReference<C> can be implicitly converted to Any and then treated as C.
                // On another hand, it can be unsafely cast from another external type, so do not
                // resolve it to constants even for `JsReference<C> is C` checks.
                // JsAny is included as it can contain actual JsReference objects.
                val argumentAsAny = narrowType(fromType, context.irBuiltIns.anyType, argument)
                return generateIsSubClassTest(argumentAsAny, toType)
            }
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

        return generateIsSubClassTest(argument, toType)
    }

    private fun generateIsSubClassTest(argument: IrExpression, toType: IrType): IrCall {
        return builder.irCall(symbols.refTest).apply {
            arguments[0] = argument
            typeArguments[0] = toType
        }
    }

    private fun generateIsExternalClass(argument: IrExpression, klass: IrClass): IrExpression {
        val instanceCheckFunction = klass.instanceCheckForExternalClass!!
        if (isExternalType(argument.type) && instanceCheckFunction.jsFunctionForExternalAdapterFunction != null) {
            return builder.irCall(instanceCheckFunction.jsFunctionForExternalAdapterFunction!!).also {
                it.arguments[0] = argument
            }
        } else {
            return builder.irCall(instanceCheckFunction).also {
                it.arguments[0] = narrowType(argument.type, context.irBuiltIns.anyType, argument) //TODO("Why we need it?)
            }
        }
    }
}
