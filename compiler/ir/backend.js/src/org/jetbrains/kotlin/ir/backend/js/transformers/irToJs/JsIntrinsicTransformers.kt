/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.KlibSymbols
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.ES6ConstructorLowering
import org.jetbrains.kotlin.ir.backend.js.lower.ES6PrimaryConstructorOptimizationLowering
import org.jetbrains.kotlin.ir.backend.js.lower.isEs6ConstructorReplacement
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.getInlineClassBackingField
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.isInlineClassBoxing
import org.jetbrains.kotlin.js.backend.ast.metadata.isInlineClassUnboxing
import org.jetbrains.kotlin.js.config.compileLongAsBigint
import org.jetbrains.kotlin.utils.filterIsInstanceAnd

private typealias IrCallTransformer<T> = (T, context: JsGenerationContext) -> JsExpression
private typealias IntrinsicMap = MutableMap<IrSymbol, IrCallTransformer<*>>

class JsIntrinsicTransformers(backendContext: JsIrBackendContext) {
    private val transformers: Map<IrSymbol, IrCallTransformer<*>>
    val icUtils = backendContext.inlineClassesUtils

    init {
        val symbols = backendContext.symbols

        transformers = hashMapOf()

        transformers.apply {
            binOp(symbols.jsEqeqeq, JsBinaryOperator.REF_EQ)
            binOp(symbols.jsNotEqeq, JsBinaryOperator.REF_NEQ)
            binOp(symbols.jsEqeq, JsBinaryOperator.EQ)
            binOp(symbols.jsNotEq, JsBinaryOperator.NEQ)

            binOp(symbols.jsGt, JsBinaryOperator.GT)
            binOp(symbols.jsGtEq, JsBinaryOperator.GTE)
            binOp(symbols.jsLt, JsBinaryOperator.LT)
            binOp(symbols.jsLtEq, JsBinaryOperator.LTE)

            prefixOp(symbols.jsNot, JsUnaryOperator.NOT)
            binOp(symbols.jsAnd, JsBinaryOperator.AND)
            binOp(symbols.jsOr, JsBinaryOperator.OR)

            prefixOp(symbols.jsUnaryPlus, JsUnaryOperator.POS)
            prefixOp(symbols.jsUnaryMinus, JsUnaryOperator.NEG)

            prefixOp(symbols.jsPrefixInc, JsUnaryOperator.INC)
            postfixOp(symbols.jsPostfixInc, JsUnaryOperator.INC)
            prefixOp(symbols.jsPrefixDec, JsUnaryOperator.DEC)
            postfixOp(symbols.jsPostfixDec, JsUnaryOperator.DEC)

            prefixOp(symbols.jsDelete, JsUnaryOperator.DELETE)

            binOp(symbols.jsPlus, JsBinaryOperator.ADD)
            binOp(symbols.jsMinus, JsBinaryOperator.SUB)
            binOp(symbols.jsMult, JsBinaryOperator.MUL)
            binOp(symbols.jsDiv, JsBinaryOperator.DIV)
            binOp(symbols.jsMod, JsBinaryOperator.MOD)

            binOp(symbols.jsPlusAssign, JsBinaryOperator.ASG_ADD)
            binOp(symbols.jsMinusAssign, JsBinaryOperator.ASG_SUB)
            binOp(symbols.jsMultAssign, JsBinaryOperator.ASG_MUL)
            binOp(symbols.jsDivAssign, JsBinaryOperator.ASG_DIV)
            binOp(symbols.jsModAssign, JsBinaryOperator.ASG_MOD)

            binOp(symbols.jsBitAnd, JsBinaryOperator.BIT_AND)
            binOp(symbols.jsBitOr, JsBinaryOperator.BIT_OR)
            binOp(symbols.jsBitXor, JsBinaryOperator.BIT_XOR)
            prefixOp(symbols.jsBitNot, JsUnaryOperator.BIT_NOT)

            binOp(symbols.jsBitShiftR, JsBinaryOperator.SHR)
            binOp(symbols.jsBitShiftRU, JsBinaryOperator.SHRU)
            binOp(symbols.jsBitShiftL, JsBinaryOperator.SHL)

            binOp(symbols.jsInstanceOf, JsBinaryOperator.INSTANCEOF)

            binOp(symbols.jsIn, JsBinaryOperator.INOP)

            prefixOp(symbols.jsTypeOf, JsUnaryOperator.TYPEOF)

            add(symbols.jsIsEs6) { _, _ -> JsBooleanLiteral(backendContext.es6mode) }

            add(symbols.jsYieldFunctionSymbol) { call, context ->
                JsYield(translateCallArguments(call, context).single())
            }

            add(symbols.jsObjectCreateSymbol) { call, context ->
                val classToCreate = call.typeArguments[0]!!.classifierOrFail.owner as IrClass
                val className = classToCreate.getClassRef(context.staticContext)
                objectCreate(prototypeOf(className, context.staticContext), context.staticContext)
            }

            add(symbols.jsClass) { call, context ->
                val typeArgument = call.typeArguments[0]
                typeArgument?.getClassRef(context.staticContext)
                    ?: compilationException(
                        "Type argument of jsClass must be statically known class",
                        typeArgument
                    )
            }

            add(symbols.jsNewTarget) { _, _ ->
                JsNameRef(JsName("target", false), JsNameRef(JsName("new", false)))
            }

            add(symbols.jsOpenInitializerBox) { call, context ->
                val arguments = translateCallArguments(call, context)

                JsInvocation(
                    JsNameRef("Object.assign"),
                    arguments
                )
            }

            add(symbols.jsEmptyObject) { _, _ ->
                JsObjectLiteral()
            }

            addIfNotNull(symbols.jsCode) { call, _ ->
                compilationException(
                    "Should not be called",
                    call
                )
            }

            add(symbols.jsArrayLength) { call, context ->
                val args = translateCallArguments(call, context)
                JsNameRef("length", args[0])
            }

            add(symbols.jsArrayGet) { call, context ->
                val args = translateCallArguments(call, context)
                val array = args[0]
                val index = args[1]
                JsArrayAccess(array, index)
            }

            add(symbols.jsArraySet) { call, context ->
                val args = translateCallArguments(call, context)
                val array = args[0]
                val index = args[1]
                val value = args[2]
                JsBinaryOperation(JsBinaryOperator.ASG, JsArrayAccess(array, index), value)
            }

            add(symbols.arrayLiteral) { call, context ->
                translateCallArguments(call, context).single()
            }

            for (intrinsic in arrayOf(
                symbols.jsArrayLike2Array,
                symbols.jsSliceArrayLikeFromIndex,
                symbols.jsSliceArrayLikeFromIndexToIndex
            )) {
                add(intrinsic) { call, context ->
                    val args = translateCallArguments(call, context)
                    JsInvocation(JsNameRef(Namer.CALL_FUNCTION, JsNameRef(Namer.SLICE_FUNCTION, JsArrayLiteral())), args)
                }
            }

            add(symbols.jsArraySlice) { call, context ->
                JsInvocation(JsNameRef(Namer.SLICE_FUNCTION, translateCallArguments(call, context).single()))
            }

            add(symbols.longCopyOfRange) { call, context ->
                val args = translateCallArguments(call, context)

                if (backendContext.configuration.compileLongAsBigint) {
                    JsInvocation(JsNameRef(Namer.SLICE_FUNCTION, args.first()), args.drop(1))
                } else {
                    JsInvocation(context.getNameForStaticFunction(symbols.longCopyOfRangeForBoxedLong!!.owner).makeRef(), args)
                }
            }

            for ((type, prefix) in symbols.primitiveToTypedArrayMap) {
                add(symbols.primitiveToSizeConstructor[type]!!) { call, context ->
                    JsNew(JsNameRef("${prefix}Array"), translateCallArguments(call, context))
                }
                add(symbols.primitiveToLiteralConstructor[type]!!) { call, context ->
                    JsNew(JsNameRef("${prefix}Array"), translateCallArguments(call, context))
                }
            }

            add(symbols.jsBoxIntrinsic) { call, context ->
                val arg = translateCallArguments(call, context).single()
                val inlineClass = call.typeArguments[0]?.let { icUtils.getRuntimeClassFor(it) }
                    ?: compilationException("Unexpected type argument in box intrinsic", call)
                val constructor = inlineClass.declarations.filterIsInstance<IrConstructor>().single { it.isPrimary }

                JsNew(constructor.getConstructorRef(context.staticContext), listOf(arg))
                    .apply { isInlineClassBoxing = true }
            }

            add(symbols.jsUnboxIntrinsic) { call, context ->
                val arg = translateCallArguments(call, context).single()
                val inlineClass = icUtils.getInlinedClass(call.typeArguments[1]!!)!!
                val field = getInlineClassBackingField(inlineClass)
                val fieldName = context.getNameForField(field)
                JsNameRef(fieldName, arg).apply { isInlineClassUnboxing = true }
            }

            add(symbols.jsCall) { call, context: JsGenerationContext ->
                val args = translateCallArguments(call, context)
                val receiver = args[0]
                val target = args[1]
                val varargs = args[2] as? JsArrayLiteral ?: error("Expect to have JsArrayLiteral, because of vararg with dynamic element type")

                val callRef = JsNameRef(Namer.CALL_FUNCTION, target)
                JsInvocation(callRef, receiver, *varargs.expressions.toTypedArray())
            }

            add(symbols.jsBind) { call, context: JsGenerationContext ->
                val receiver = call.arguments[0]!!
                val jsReceiver = receiver.accept(IrElementToJsExpressionTransformer(), context)
                val jsBindTarget = when (val target = call.arguments[1]!!) {
                    is IrFunctionReference -> {
                        val superClass = call.superQualifierSymbol!!
                        val functionName = context.getNameForMemberFunction(target.symbol.owner as IrSimpleFunction)
                        val superName = superClass.owner.getClassRef(context.staticContext)
                        JsNameRef(functionName, prototypeOf(superName, context.staticContext))
                    }
                    is IrFunctionExpression -> target.accept(IrElementToJsExpressionTransformer(), context)
                    else -> compilationException(
                        "The 'target' argument of 'jsBind' must be either IrFunctionReference or IrFunctionExpression",
                        call
                    )
                }
                val bindRef = JsNameRef(Namer.BIND_FUNCTION, jsBindTarget)
                JsInvocation(bindRef, jsReceiver)
            }

            add(symbols.jsContexfulRef) { call, context: JsGenerationContext ->
                val receiver = call.arguments[0]!!
                val jsReceiver = receiver.accept(IrElementToJsExpressionTransformer(), context)
                val target = call.arguments[1] as IrRawFunctionReference
                val jsTarget = context.getNameForMemberFunction(target.symbol.owner as IrSimpleFunction)

                JsNameRef(jsTarget, jsReceiver)
            }

            add(symbols.unreachable) { _, _ ->
                JsInvocation(JsNameRef(Namer.UNREACHABLE_NAME))
            }

            /**
             * We don't use [KlibSymbols.SharedVariableBoxClassInfo.constructor] here
             * because in ES6 it may be replaced by a factory function (see [ES6ConstructorLowering]),
             * after which replaced again by a new constructor as an optimization (see [ES6PrimaryConstructorOptimizationLowering]).
             */
            val sharedVariableBoxConstructors = symbols.genericSharedVariableBox.klass.owner
                .declarations
                .filterIsInstanceAnd<IrFunction> {
                    it is IrConstructor || it.isEs6ConstructorReplacement
                }
                .map { it.symbol }

            addAll(sharedVariableBoxConstructors) { call, context ->
                val arg = translateCallArguments(call, context).single()
                JsObjectLiteral(listOf(JsPropertyInitializer(JsStringLiteral(Namer.SHARED_BOX_V), arg)))
            }

            add(symbols.genericSharedVariableBox.load) { call, context: JsGenerationContext ->
                val box = translateDispatchArgument(call, context)
                JsNameRef(Namer.SHARED_BOX_V, box)
            }

            add(symbols.genericSharedVariableBox.store) { call, context: JsGenerationContext ->
                val box = translateDispatchArgument(call, context)
                val value = translateCallArguments(call, context).single()
                jsAssignment(JsNameRef(Namer.SHARED_BOX_V, box), value)
            }

            val suspendInvokeTransform: (IrCall, JsGenerationContext) -> JsExpression = { call, context: JsGenerationContext ->
                // Because it is intrinsic, we know everything about this function
                // There is callable reference as extension receiver
                val invokeFun = invokeFunForLambda(call)

                val jsInvokeFunName = context.getNameForMemberFunction(invokeFun)

                val args = translateCallArguments(call, context)
                JsInvocation(JsNameRef(jsInvokeFunName, args[0]), args.drop(1))
            }

            add(symbols.jsInvokeSuspendSuperType, suspendInvokeTransform)
            add(symbols.jsInvokeSuspendSuperTypeWithReceiver, suspendInvokeTransform)
            add(symbols.jsInvokeSuspendSuperTypeWithReceiverAndParam, suspendInvokeTransform)

            add(symbols.jsArguments) { _, _ -> Namer.ARGUMENTS }

            add(symbols.jsNewAnonymousClass) { call, context ->
                val baseClass = translateCallArguments(call, context).single() as JsNameRef
                JsClass(baseClass = baseClass)
            }

            add(symbols.void.owner.getter!!.symbol) { _, context ->
                val backingField = context.getNameForField(symbols.void.owner.backingField!!)
                JsNameRef(backingField)
            }

            add(symbols.suspendOrReturnFunctionSymbol) { call, context ->
                val (generatorCall, continuation) = translateCallArguments(call, context)
                val jsInvokeFunName = context.getNameForStaticFunction(call.symbol.owner)
                val VOID = context.getNameForField(symbols.void.owner.backingField!!)
                val generatorBindCall = (generatorCall as JsInvocation).let {
                    JsInvocation(JsNameRef(Namer.BIND_FUNCTION, it.qualifier), listOf(JsNameRef(VOID)) + it.arguments.dropLast(1))
                }
                JsInvocation(JsNameRef(jsInvokeFunName), generatorBindCall, continuation)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun get(symbol: IrSimpleFunctionSymbol): IrCallTransformer<IrCall>? =
        transformers[symbol] as IrCallTransformer<IrCall>?

    @Suppress("UNCHECKED_CAST")
    operator fun get(symbol: IrConstructorSymbol): IrCallTransformer<IrConstructorCall>? =
        transformers[symbol] as IrCallTransformer<IrConstructorCall>?
}

private fun translateDispatchArgument(
    expression: IrFunctionAccessExpression,
    context: JsGenerationContext,
): JsExpression = expression.dispatchReceiver?.accept(IrElementToJsExpressionTransformer(), context)
    ?: compilationException("Expected dispatch receiver", expression)

private fun translateCallArguments(
    expression: IrFunctionAccessExpression,
    context: JsGenerationContext,
): List<JsExpression> {
    return translateNonDispatchCallArguments(expression, context, IrElementToJsExpressionTransformer(), false).map { it.jsArgument }
}

private fun IntrinsicMap.add(functionSymbol: IrSimpleFunctionSymbol, t: IrCallTransformer<IrCall>) {
    put(functionSymbol, t)
}

private fun IntrinsicMap.add(functionSymbol: IrConstructorSymbol, t: IrCallTransformer<IrConstructorCall>) {
    put(functionSymbol, t)
}

private fun IntrinsicMap.addIfNotNull(symbol: IrSimpleFunctionSymbol?, t: IrCallTransformer<IrCall>) {
    if (symbol == null) return
    put(symbol, t)
}

private fun IntrinsicMap.addAll(symbols: Iterable<IrFunctionSymbol>, t: IrCallTransformer<IrFunctionAccessExpression>) {
    for (symbol in symbols) {
        put(symbol, t)
    }
}

private fun IntrinsicMap.binOp(function: IrSimpleFunctionSymbol, op: JsBinaryOperator) {
    withTranslatedArgs(function) { JsBinaryOperation(op, it[0], it[1]) }
}

private fun IntrinsicMap.prefixOp(function: IrSimpleFunctionSymbol, op: JsUnaryOperator) {
    withTranslatedArgs(function) { JsPrefixOperation(op, it[0]) }
}

private fun IntrinsicMap.postfixOp(function: IrSimpleFunctionSymbol, op: JsUnaryOperator) {
    withTranslatedArgs(function) { JsPostfixOperation(op, it[0]) }
}

private inline fun IntrinsicMap.withTranslatedArgs(
    function: IrSimpleFunctionSymbol,
    crossinline t: (List<JsExpression>) -> JsExpression
) {
    put(function) { call: IrCall, context -> t(translateCallArguments(call, context)) }
}
