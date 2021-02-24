/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.Namer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.getInlineClassBackingField
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.js.backend.ast.*

typealias IrCallTransformer = (IrCall, context: JsGenerationContext) -> JsExpression

class JsIntrinsicTransformers(backendContext: JsIrBackendContext) {
    private val transformers: Map<IrSymbol, IrCallTransformer>
    val icUtils = backendContext.inlineClassesUtils

    init {
        val intrinsics = backendContext.intrinsics

        transformers = mutableMapOf()

        transformers.apply {
            binOp(intrinsics.jsEqeqeq, JsBinaryOperator.REF_EQ)
            binOp(intrinsics.jsNotEqeq, JsBinaryOperator.REF_NEQ)
            binOp(intrinsics.jsEqeq, JsBinaryOperator.EQ)
            binOp(intrinsics.jsNotEq, JsBinaryOperator.NEQ)

            binOp(intrinsics.jsGt, JsBinaryOperator.GT)
            binOp(intrinsics.jsGtEq, JsBinaryOperator.GTE)
            binOp(intrinsics.jsLt, JsBinaryOperator.LT)
            binOp(intrinsics.jsLtEq, JsBinaryOperator.LTE)

            prefixOp(intrinsics.jsNot, JsUnaryOperator.NOT)
            binOp(intrinsics.jsAnd, JsBinaryOperator.AND)
            binOp(intrinsics.jsOr, JsBinaryOperator.OR)

            prefixOp(intrinsics.jsUnaryPlus, JsUnaryOperator.POS)
            prefixOp(intrinsics.jsUnaryMinus, JsUnaryOperator.NEG)

            prefixOp(intrinsics.jsPrefixInc, JsUnaryOperator.INC)
            postfixOp(intrinsics.jsPostfixInc, JsUnaryOperator.INC)
            prefixOp(intrinsics.jsPrefixDec, JsUnaryOperator.DEC)
            postfixOp(intrinsics.jsPostfixDec, JsUnaryOperator.DEC)

            binOp(intrinsics.jsPlus, JsBinaryOperator.ADD)
            binOp(intrinsics.jsMinus, JsBinaryOperator.SUB)
            binOp(intrinsics.jsMult, JsBinaryOperator.MUL)
            binOp(intrinsics.jsDiv, JsBinaryOperator.DIV)
            binOp(intrinsics.jsMod, JsBinaryOperator.MOD)

            binOp(intrinsics.jsPlusAssign, JsBinaryOperator.ASG_ADD)
            binOp(intrinsics.jsMinusAssign, JsBinaryOperator.ASG_SUB)
            binOp(intrinsics.jsMultAssign, JsBinaryOperator.ASG_MUL)
            binOp(intrinsics.jsDivAssign, JsBinaryOperator.ASG_DIV)
            binOp(intrinsics.jsModAssign, JsBinaryOperator.ASG_MOD)

            binOp(intrinsics.jsBitAnd, JsBinaryOperator.BIT_AND)
            binOp(intrinsics.jsBitOr, JsBinaryOperator.BIT_OR)
            binOp(intrinsics.jsBitXor, JsBinaryOperator.BIT_XOR)
            prefixOp(intrinsics.jsBitNot, JsUnaryOperator.BIT_NOT)

            binOp(intrinsics.jsBitShiftR, JsBinaryOperator.SHR)
            binOp(intrinsics.jsBitShiftRU, JsBinaryOperator.SHRU)
            binOp(intrinsics.jsBitShiftL, JsBinaryOperator.SHL)

            binOp(intrinsics.jsInstanceOf, JsBinaryOperator.INSTANCEOF)

            prefixOp(intrinsics.jsTypeOf, JsUnaryOperator.TYPEOF)

            add(intrinsics.jsObjectCreate) { call, context ->
                val classToCreate = call.getTypeArgument(0)!!.classifierOrFail.owner as IrClass
                val className = context.getNameForClass(classToCreate)
                val prototype = prototypeOf(className.makeRef())
                JsInvocation(Namer.JS_OBJECT_CREATE_FUNCTION, prototype)
            }

            add(intrinsics.jsClass) { call, context ->
                val typeArgument = call.getTypeArgument(0)
                val classSymbol = typeArgument?.classOrNull
                    ?: error("Type argument of jsClass must be statically known class, but " + typeArgument?.render())

                val klass = classSymbol.owner

                when {
                    klass.isEffectivelyExternal() ->
                        context.getRefForExternalClass(klass)

                    else ->
                        context.getNameForStaticDeclaration(klass as IrDeclarationWithName).makeRef()
                }
            }

            add(intrinsics.jsNewTarget) { _, _ ->
                JsNameRef(JsName("target"), JsNameRef(JsName("new")))
            }

            add(intrinsics.jsOpenInitializerBox) { call, context ->
                val arguments = translateCallArguments(call, context)

                JsInvocation(
                    JsNameRef("Object.assign"),
                    arguments
                )
            }

            add(intrinsics.jsEmptyObject) { _, _ ->
                JsObjectLiteral()
            }

            add(intrinsics.es6DefaultType) { call, context ->
                val classifier: IrClassifierSymbol = call.getTypeArgument(0)!!.classifierOrFail
                val owner = classifier.owner

                when {
                    owner is IrClass && owner.isEffectivelyExternal() ->
                        context.getRefForExternalClass(owner)

                    else ->
                        context.getNameForStaticDeclaration(owner as IrDeclarationWithName).makeRef()
                }
            }

            addIfNotNull(intrinsics.jsCode) { _, _ -> error("Should not be called") }

            add(intrinsics.jsGetContinuation) { _, context: JsGenerationContext ->
                context.continuation
            }

            add(backendContext.ir.symbols.returnIfSuspended) { call, context ->
                val args = translateCallArguments(call, context)
                args[0]
            }

            add(intrinsics.jsCoroutineContext) { _, context: JsGenerationContext ->
                val contextGetter = backendContext.coroutineGetContext
                val getterName = context.getNameForStaticFunction(contextGetter.owner)
                val continuation = context.continuation
                JsInvocation(JsNameRef(getterName, continuation))
            }

            add(intrinsics.jsArrayLength) { call, context ->
                val args = translateCallArguments(call, context)
                JsNameRef("length", args[0])
            }

            add(intrinsics.jsArrayGet) { call, context ->
                val args = translateCallArguments(call, context)
                val array = args[0]
                val index = args[1]
                JsArrayAccess(array, index)
            }

            add(intrinsics.jsArraySet) { call, context ->
                val args = translateCallArguments(call, context)
                val array = args[0]
                val index = args[1]
                val value = args[2]
                JsBinaryOperation(JsBinaryOperator.ASG, JsArrayAccess(array, index), value)
            }

            add(intrinsics.arrayLiteral) { call, context ->
                translateCallArguments(call, context).single()
            }

            add(intrinsics.jsArraySlice) { call, context ->
                JsInvocation(JsNameRef(Namer.SLICE_FUNCTION, translateCallArguments(call, context).single()))
            }

            for ((type, prefix) in intrinsics.primitiveToTypedArrayMap) {
                add(intrinsics.primitiveToSizeConstructor[type]!!) { call, context ->
                    JsNew(JsNameRef("${prefix}Array"), translateCallArguments(call, context))
                }
                add(intrinsics.primitiveToLiteralConstructor[type]!!) { call, context ->
                    JsNew(JsNameRef("${prefix}Array"), translateCallArguments(call, context))
                }
            }

            add(intrinsics.jsBoxIntrinsic) { call, context ->
                val arg = translateCallArguments(call, context).single()
                val inlineClass = icUtils.getInlinedClass(call.getTypeArgument(0)!!)!!
                val constructor = inlineClass.declarations.filterIsInstance<IrConstructor>().single { it.isPrimary }
                JsNew(context.getNameForConstructor(constructor).makeRef(), listOf(arg))
            }

            add(intrinsics.jsUnboxIntrinsic) { call, context ->
                val arg = translateCallArguments(call, context).single()
                val inlineClass = icUtils.getInlinedClass(call.getTypeArgument(1)!!)!!
                val field = getInlineClassBackingField(inlineClass)
                val fieldName = context.getNameForField(field)
                JsNameRef(fieldName, arg)
            }

            add(intrinsics.jsBind) { call, context: JsGenerationContext ->
                val receiver = call.getValueArgument(0)!!
                val reference = call.getValueArgument(1) as IrFunctionReference
                val superClass = call.superQualifierSymbol!!

                val jsReceiver = receiver.accept(IrElementToJsExpressionTransformer(), context)
                val functionName = context.getNameForMemberFunction(reference.symbol.owner as IrSimpleFunction)
                val superName = context.getNameForClass(superClass.owner).makeRef()
                val qPrototype = JsNameRef(functionName, prototypeOf(superName))
                val bindRef = JsNameRef(Namer.BIND_FUNCTION, qPrototype)

                JsInvocation(bindRef, jsReceiver)
            }

            add(intrinsics.unreachable) { _, _ ->
                JsInvocation(JsNameRef(Namer.UNREACHABLE_NAME))
            }

            add(intrinsics.createSharedBox) { call, context: JsGenerationContext ->
                val arg = translateCallArguments(call, context).single()
                JsObjectLiteral(listOf(JsPropertyInitializer(JsNameRef(Namer.SHARED_BOX_V), arg)))
            }

            add(intrinsics.readSharedBox) { call, context: JsGenerationContext ->
                val box = translateCallArguments(call, context).single()
                JsNameRef(Namer.SHARED_BOX_V, box)
            }

            add(intrinsics.writeSharedBox) { call, context: JsGenerationContext ->
                val args = translateCallArguments(call, context)
                val box = args[0]
                val value = args[1]
                jsAssignment(JsNameRef(Namer.SHARED_BOX_V, box), value)
            }
            add(intrinsics.jsUndefined) { _, _ ->
                JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(1))
            }
        }
    }

    operator fun get(symbol: IrSymbol): IrCallTransformer? = transformers[symbol]
}

private fun translateCallArguments(expression: IrCall, context: JsGenerationContext): List<JsExpression> {
    return translateCallArguments(expression, context, IrElementToJsExpressionTransformer())
}

private fun MutableMap<IrSymbol, IrCallTransformer>.add(functionSymbol: IrSymbol, t: IrCallTransformer) {
    put(functionSymbol, t)
}

private fun MutableMap<IrSymbol, IrCallTransformer>.add(function: IrSimpleFunction, t: IrCallTransformer) {
    put(function.symbol, t)
}

private fun MutableMap<IrSymbol, IrCallTransformer>.addIfNotNull(symbol: IrSymbol?, t: IrCallTransformer) {
    if (symbol == null) return
    put(symbol, t)
}

private fun MutableMap<IrSymbol, IrCallTransformer>.binOp(function: IrSimpleFunctionSymbol, op: JsBinaryOperator) {
    withTranslatedArgs(function) { JsBinaryOperation(op, it[0], it[1]) }
}

private fun MutableMap<IrSymbol, IrCallTransformer>.prefixOp(function: IrSimpleFunctionSymbol, op: JsUnaryOperator) {
    withTranslatedArgs(function) { JsPrefixOperation(op, it[0]) }
}

private fun MutableMap<IrSymbol, IrCallTransformer>.postfixOp(function: IrSimpleFunctionSymbol, op: JsUnaryOperator) {
    withTranslatedArgs(function) { JsPostfixOperation(op, it[0]) }
}

private inline fun MutableMap<IrSymbol, IrCallTransformer>.withTranslatedArgs(
    function: IrSimpleFunctionSymbol,
    crossinline t: (List<JsExpression>) -> JsExpression
) {
    put(function) { call, context -> t(translateCallArguments(call, context)) }
}
