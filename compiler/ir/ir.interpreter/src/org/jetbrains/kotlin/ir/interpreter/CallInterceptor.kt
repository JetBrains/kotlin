/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.interpreter.builtins.evaluateIntrinsicAnnotation
import org.jetbrains.kotlin.ir.interpreter.builtins.interpretBinaryFunction
import org.jetbrains.kotlin.ir.interpreter.builtins.interpretTernaryFunction
import org.jetbrains.kotlin.ir.interpreter.builtins.interpretUnaryFunction
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.interpreter.intrinsics.IntrinsicEvaluator
import org.jetbrains.kotlin.ir.interpreter.proxy.wrap
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KFunctionState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KTypeState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.ReflectionState
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.lang.invoke.MethodHandle

internal interface CallInterceptor {
    val environment: IrInterpreterEnvironment
    val interpreter: IrInterpreter
    fun interceptCall(call: IrCall, irFunction: IrFunction, receiver: State?, args: List<State>)
    fun interceptConstructor(constructorCall: IrFunctionAccessExpression, receiver: State, args: List<State>)
    fun interceptGetObjectValue(expression: IrGetObjectValue)
    fun interceptEnumEntry(enumEntry: IrEnumEntry)
    fun interceptJavaStaticField(expression: IrGetField)
}

internal class DefaultCallInterceptor(override val interpreter: IrInterpreter) : CallInterceptor {
    override val environment: IrInterpreterEnvironment = interpreter.environment
    private val callStack: CallStack
        get() = environment.callStack
    private val irBuiltIns: IrBuiltIns
        get() = environment.irBuiltIns
    private val bodyMap: Map<IdSignature, IrBody>
        get() = interpreter.bodyMap

    override fun interceptCall(call: IrCall, irFunction: IrFunction, receiver: State?, args: List<State>) {
        val isInlineOnly = irFunction.hasAnnotation(FqName("kotlin.internal.InlineOnly"))
        val originalCallName = call.symbol.owner.name.asString()
        when {
            receiver is Wrapper && !isInlineOnly -> receiver.getMethod(irFunction).invokeMethod(irFunction, args)
            irFunction.hasAnnotation(evaluateIntrinsicAnnotation) -> Wrapper.getStaticMethod(irFunction).invokeMethod(irFunction, args)
            receiver is KFunctionState && originalCallName == "invoke" -> callStack.addInstruction(CompoundInstruction(irFunction))
            receiver is ReflectionState -> Wrapper.getReflectionMethod(irFunction).invokeMethod(irFunction, args)
            receiver is Primitive<*> -> calculateBuiltIns(irFunction, args) // check for js char, js long and get field for primitives
            irFunction.body is IrSyntheticBody -> handleIntrinsicMethods(irFunction)
            irFunction.body == null ->
                irFunction.trySubstituteFunctionBody() ?: irFunction.tryCalculateLazyConst() ?: calculateBuiltIns(irFunction, args)
            else -> callStack.addInstruction(CompoundInstruction(irFunction))
        }
    }

    override fun interceptConstructor(constructorCall: IrFunctionAccessExpression, receiver: State, args: List<State>) {
        val constructor = constructorCall.symbol.owner
        val irClass = constructor.parentAsClass
        when {
            irClass.hasAnnotation(evaluateIntrinsicAnnotation) || irClass.fqNameWhenAvailable!!.startsWith(Name.identifier("java")) -> {
                Wrapper.getConstructorMethod(constructor).invokeMethod(constructor, args)
                if (constructorCall !is IrConstructorCall) (receiver as Common).superWrapperClass = callStack.popState() as Wrapper
            }
            irClass.defaultType.isArray() || irClass.defaultType.isPrimitiveArray() -> {
                // array constructor doesn't have body so must be treated separately
                callStack.addVariable(Variable(constructor.symbol, KTypeState(constructorCall.type, irBuiltIns.anyClass.owner)))
                handleIntrinsicMethods(constructor)
            }
            irClass.defaultType.isUnsignedType() -> {
                val propertySymbol = irClass.declarations.single { it is IrProperty }.symbol
                callStack.pushState(receiver.apply { fields += Variable(propertySymbol, args.single()) })
            }
            else -> {
                callStack.pushState(receiver)
                callStack.addInstruction(CompoundInstruction(constructor))
            }
        }
    }

    override fun interceptGetObjectValue(expression: IrGetObjectValue) {
        val objectClass = expression.symbol.owner
        if (objectClass.hasAnnotation(evaluateIntrinsicAnnotation)) {
            val result = Wrapper.getCompanionObject(objectClass)
            environment.mapOfObjects[expression.symbol] = result
            callStack.pushState(result)
        }
    }

    override fun interceptEnumEntry(enumEntry: IrEnumEntry) {
        val enumClass = enumEntry.symbol.owner.parentAsClass

        when {
            enumClass.hasAnnotation(evaluateIntrinsicAnnotation) -> {
                val enumEntryName = enumEntry.name.asString().toState(environment.irBuiltIns.stringType)
                val valueOfFun = enumClass.declarations.single { it.nameForIrSerialization.asString() == "valueOf" } as IrFunction
                Wrapper.getEnumEntry(enumClass)!!.invokeMethod(valueOfFun, listOf(enumEntryName))
                environment.mapOfEnums[enumEntry.symbol] = callStack.popState() as Complex
            }
            else -> {
                val enumSuperCall = (enumClass.primaryConstructor?.body?.statements?.firstOrNull() as? IrEnumConstructorCall)
                enumSuperCall?.apply { (0 until this.valueArgumentsCount).forEach { putValueArgument(it, null) } } // restore to null
                callStack.dropSubFrame()
            }
        }
    }

    override fun interceptJavaStaticField(expression: IrGetField) {
        val field = expression.symbol.owner
        assert(field.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && field.isStatic)
        assert(field.initializer?.expression !is IrConst<*>)
        callStack.pushState(Wrapper.getStaticGetter(field)!!.invokeWithArguments().toState(field.type))
    }

    private fun MethodHandle?.invokeMethod(irFunction: IrFunction, args: List<State>) {
        this ?: return handleIntrinsicMethods(irFunction)
        val argsForMethodInvocation = irFunction.getArgsForMethodInvocation(interpreter, this.type(), args)
        withExceptionHandler(environment) {
            val result = this.invokeWithArguments(argsForMethodInvocation)
            callStack.pushState(result.toState(result.getType(irFunction.returnType)))
        }
    }

    private fun handleIntrinsicMethods(irFunction: IrFunction) {
        IntrinsicEvaluator.unwindInstructions(irFunction, environment).forEach { callStack.addInstruction(it) }
    }

    private fun calculateBuiltIns(irFunction: IrFunction, args: List<State>) {
        val methodName = when (val property = (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol) {
            null -> irFunction.name.asString()
            else -> property.owner.name.asString()
        }

        val receiverType = irFunction.dispatchReceiverParameter?.type
        val argsType = listOfNotNull(receiverType) + irFunction.valueParameters.map { it.type }
        val argsValues = args.map { it.wrap(interpreter, calledFromBuiltIns = methodName !in setOf("plus", IrBuiltIns.OperatorNames.EQEQ)) }

        fun IrType.getOnlyName(): String {
            return when {
                this.originalKotlinType != null -> this.originalKotlinType.toString()
                this is IrSimpleType -> (this.classifierOrFail.owner as IrDeclarationWithName).name.asString() + (if (this.hasQuestionMark) "?" else "")
                else -> this.render()
            }
        }

        // TODO replace unary, binary, ternary functions with vararg
        withExceptionHandler(environment) {
            val result = when (argsType.size) {
                1 -> interpretUnaryFunction(methodName, argsType[0].getOnlyName(), argsValues[0])
                2 -> when (methodName) {
                    "rangeTo" -> return calculateRangeTo(irFunction.returnType, args)
                    else -> interpretBinaryFunction(
                        methodName, argsType[0].getOnlyName(), argsType[1].getOnlyName(), argsValues[0], argsValues[1]
                    )
                }
                3 -> interpretTernaryFunction(
                    methodName, argsType[0].getOnlyName(), argsType[1].getOnlyName(), argsType[2].getOnlyName(),
                    argsValues[0], argsValues[1], argsValues[2]
                )
                else -> throw InterpreterError("Unsupported number of arguments for invocation as builtin functions")
            }
            // TODO check "result is Unit"
            callStack.pushState(result.toState(result.getType(irFunction.returnType)))
        }
    }

    private fun calculateRangeTo(type: IrType, args: List<State>) {
        val constructor = type.classOrNull!!.owner.constructors.first()
        val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)
        val constructorValueParameters = constructor.valueParameters.map { it.symbol }

        val primitiveValueParameters = args.map { it as Primitive<*> }
        primitiveValueParameters.forEachIndexed { index, primitive ->
            constructorCall.putValueArgument(index, primitive.value.toIrConst(constructorValueParameters[index].owner.type))
        }

        callStack.addInstruction(CompoundInstruction(constructorCall))
    }

    private fun Any?.getType(defaultType: IrType): IrType {
        return when (this) {
            is Boolean -> irBuiltIns.booleanType
            is Char -> irBuiltIns.charType
            is Byte -> irBuiltIns.byteType
            is Short -> irBuiltIns.shortType
            is Int -> irBuiltIns.intType
            is Long -> irBuiltIns.longType
            is String -> irBuiltIns.stringType
            is Float -> irBuiltIns.floatType
            is Double -> irBuiltIns.doubleType
            null -> irBuiltIns.nothingNType
            else -> defaultType
        }
    }

    private fun IrFunction.trySubstituteFunctionBody(): IrElement? {
        val signature = this.symbol.signature ?: return null
        this.body = bodyMap[signature] ?: return null
        callStack.addInstruction(CompoundInstruction(this))
        return body
    }

    // TODO fix in FIR2IR; const val getter must have body with IrGetField node
    private fun IrFunction.tryCalculateLazyConst(): IrExpression? {
        if (this !is IrSimpleFunction) return null
        val expression = this.correspondingPropertySymbol?.owner?.backingField?.initializer?.expression
        return expression?.apply { callStack.addInstruction(CompoundInstruction(this)) }
    }
}