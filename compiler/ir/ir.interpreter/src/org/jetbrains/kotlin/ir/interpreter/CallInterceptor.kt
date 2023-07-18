/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.builtins.interpretBinaryFunction
import org.jetbrains.kotlin.ir.interpreter.builtins.interpretTernaryFunction
import org.jetbrains.kotlin.ir.interpreter.builtins.interpretUnaryFunction
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.interpreter.exceptions.verify
import org.jetbrains.kotlin.ir.interpreter.exceptions.withExceptionHandler
import org.jetbrains.kotlin.ir.interpreter.intrinsics.IntrinsicEvaluator
import org.jetbrains.kotlin.ir.interpreter.proxy.wrap
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isUnsignedType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.isJs
import java.lang.invoke.MethodHandle

internal interface CallInterceptor {
    val environment: IrInterpreterEnvironment
    val irBuiltIns: IrBuiltIns
    val interpreter: IrInterpreter

    fun interceptProxy(irFunction: IrFunction, valueArguments: List<State>, expectedResultClass: Class<*> = Any::class.java): Any?
    fun interceptCall(call: IrCall, irFunction: IrFunction, args: List<State>, defaultAction: () -> Unit)
    fun interceptConstructor(constructorCall: IrFunctionAccessExpression, args: List<State>, defaultAction: () -> Unit)
    fun interceptGetObjectValue(expression: IrGetObjectValue, defaultAction: () -> Unit)
    fun interceptEnumEntry(enumEntry: IrEnumEntry, defaultAction: () -> Unit)
    fun interceptJavaStaticField(expression: IrGetField)
}

internal class DefaultCallInterceptor(override val interpreter: IrInterpreter) : CallInterceptor {
    override val environment: IrInterpreterEnvironment = interpreter.environment
    private val callStack: CallStack = environment.callStack
    override val irBuiltIns: IrBuiltIns = environment.irBuiltIns
    private val bodyMap: Map<IdSignature, IrBody> = interpreter.bodyMap

    override fun interceptProxy(irFunction: IrFunction, valueArguments: List<State>, expectedResultClass: Class<*>): Any? {
        val irCall = irFunction.createCall()
        return interpreter.withNewCallStack(irCall) {
            this@withNewCallStack.environment.callStack.pushSimpleInstruction(irCall)
            valueArguments.forEach { this@withNewCallStack.environment.callStack.pushState(it) }
        }.wrap(this@DefaultCallInterceptor, remainArraysAsIs = false, extendFrom = expectedResultClass)
    }

    override fun interceptCall(call: IrCall, irFunction: IrFunction, args: List<State>, defaultAction: () -> Unit) {
        val isInlineOnly = irFunction.hasAnnotation(FqName("kotlin.internal.InlineOnly"))
        val isSyntheticDefault = irFunction.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER
        val receiver = if (irFunction.dispatchReceiverParameter != null) args[0] else null
        when {
            receiver is Wrapper && !isInlineOnly && !isSyntheticDefault -> receiver.getMethod(irFunction).invokeMethod(irFunction, args)
            Wrapper.mustBeHandledWithWrapper(irFunction) -> Wrapper.getStaticMethod(irFunction).invokeMethod(irFunction, args)
            handleIntrinsicMethods(irFunction) -> return
            receiver.mustBeHandledAsReflection(call) -> Wrapper.getReflectionMethod(irFunction).invokeMethod(irFunction, args)
            receiver is Primitive<*> -> calculateBuiltIns(irFunction, args) // check for js char, js long and get field for primitives
            // TODO try to save fields in Primitive -> then it is possible to move up next branch
            // TODO try to create backing field if it is missing
            irFunction.body == null && irFunction.isAccessorOfPropertyWithBackingField() -> callStack.pushCompoundInstruction(irFunction.createGetField())
            irFunction.body == null -> irFunction.trySubstituteFunctionBody() ?: calculateBuiltIns(irFunction, args)
            else -> defaultAction()
        }
    }

    override fun interceptConstructor(constructorCall: IrFunctionAccessExpression, args: List<State>, defaultAction: () -> Unit) {
        val receiver = callStack.loadState(constructorCall.getThisReceiver())
        val irConstructor = constructorCall.symbol.owner
        val irClass = irConstructor.parentAsClass
        when {
            Wrapper.mustBeHandledWithWrapper(irClass) -> {
                Wrapper.getConstructorMethod(irConstructor).invokeMethod(irConstructor, args)
                when {
                    irClass.isSubclassOfThrowable() -> (receiver as ExceptionState).copyFieldsFrom(callStack.popState() as Wrapper)
                    constructorCall is IrConstructorCall -> callStack.rewriteState(constructorCall.getThisReceiver(), callStack.popState())
                    else -> (receiver as Complex).superWrapperClass = callStack.popState() as Wrapper
                }
            }
            irClass.defaultType.isArray() || irClass.defaultType.isPrimitiveArray() -> {
                // array constructor doesn't have body so must be treated separately
                verify(handleIntrinsicMethods(irConstructor)) { "Unsupported intrinsic constructor: ${irConstructor.render()}" }
            }
            irClass.defaultType.isUnsignedType() -> {
                val propertyName = irClass.inlineClassRepresentation?.underlyingPropertyName
                val propertySymbol = irClass.declarations.filterIsInstance<IrProperty>()
                    .single { it.name == propertyName && it.getter?.extensionReceiverParameter == null }
                    .symbol
                callStack.pushState(receiver.apply { this.setField(propertySymbol, args.single()) })
            }
            else -> defaultAction()
        }
    }

    override fun interceptGetObjectValue(expression: IrGetObjectValue, defaultAction: () -> Unit) {
        val objectClass = expression.symbol.owner
        when {
            Wrapper.mustBeHandledWithWrapper(objectClass) -> {
                val result = Wrapper.getCompanionObject(objectClass, environment)
                environment.mapOfObjects[expression.symbol] = result
                callStack.pushState(result)
            }
            else -> defaultAction()
        }
    }

    override fun interceptEnumEntry(enumEntry: IrEnumEntry, defaultAction: () -> Unit) {
        val enumClass = enumEntry.symbol.owner.parentAsClass
        when {
            Wrapper.mustBeHandledWithWrapper(enumClass) -> {
                val enumEntryName = environment.convertToState(enumEntry.name.asString(), environment.irBuiltIns.stringType)
                val valueOfFun = enumClass.functions.single { it.name.asString() == "valueOf" }
                Wrapper.getEnumEntry(enumClass).invokeMethod(valueOfFun, listOf(enumEntryName))
                environment.mapOfEnums[enumEntry.symbol] = callStack.popState() as Complex
            }
            else -> defaultAction()
        }
    }

    override fun interceptJavaStaticField(expression: IrGetField) {
        val field = expression.symbol.owner
        verify(field.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && field.isStatic)
        verify(field.initializer?.expression !is IrConst<*>)
        callStack.pushState(environment.convertToState(Wrapper.getStaticGetter(field).invokeWithArguments(), field.type))
    }

    private fun MethodHandle?.invokeMethod(irFunction: IrFunction, args: List<State>) {
        this ?: return verify(handleIntrinsicMethods(irFunction)) { "Unsupported intrinsic function: ${irFunction.render()}" }
        val argsForMethodInvocation = irFunction.getArgsForMethodInvocation(this@DefaultCallInterceptor, this.type(), args)
        withExceptionHandler(environment) {
            val result = this.invokeWithArguments(argsForMethodInvocation) // TODO if null return Unit
            callStack.pushState(environment.convertToState(result, result.getType(irFunction.returnType)))
        }
    }

    private fun handleIntrinsicMethods(irFunction: IrFunction): Boolean {
        val instructions = IntrinsicEvaluator.unwindInstructions(irFunction, environment) ?: return false
        instructions.forEach { callStack.pushInstruction(it) }
        return true
    }

    private data class Signature(var name: String, var args: List<Arg>)
    private data class Arg(var type: String, var value: Any?)

    private fun calculateBuiltIns(irFunction: IrFunction, args: List<State>) {
        val methodName = when (val property = irFunction.property) {
            null -> irFunction.name.asString()
            else -> property.name.asString()
        }

        val receiverType = irFunction.dispatchReceiverParameter?.type ?: irFunction.extensionReceiverParameter?.type
        val argsType = (listOfNotNull(receiverType) + irFunction.valueParameters.map { it.type }).map { it.fqNameWithNullability() }
        val argsValues = args.wrap(this, irFunction)

        withExceptionHandler(environment) {
            if (methodName == "rangeTo") return calculateRangeTo(irFunction.returnType, args)
            val result = interpretBuiltinFunction(Signature(methodName, argsType.zip(argsValues).map { Arg(it.first, it.second) }))
            // TODO check "result is Unit"
            callStack.pushState(environment.convertToState(result, result.getType(irFunction.returnType)))
        }
    }

    private fun interpretBuiltinFunction(signature: Signature): Any? {
        if (environment.configuration.platform.isJs()) {
            if (signature.name == "toString") return signature.args[0].value.specialToStringForJs()
            if (signature.name == "toFloat") signature.name = "toDouble"
            signature.args
                .filter { it.value is Float || it.type == "kotlin.Float" || it.type == "kotlin.Float?" }
                .forEach {
                    it.type = when (it.type) {
                        "kotlin.Float" -> "kotlin.Double"
                        "kotlin.Float?" -> "kotlin.Double?"
                        else -> it.type
                    }
                    it.value = it.value.toString().toDouble()
                }
        }

        val name = signature.name
        val args = signature.args
        return when (args.size) {
            1 -> interpretUnaryFunction(name, args[0].type, args[0].value)
            2 -> interpretBinaryFunction(name, args[0].type, args[1].type, args[0].value, args[1].value)
            3 -> interpretTernaryFunction(name, args[0].type, args[1].type, args[2].type, args[0].value, args[1].value, args[2].value)
            else -> throw InterpreterError("Unsupported number of arguments for invocation as builtin function: $name")
        }
    }

    private fun calculateRangeTo(type: IrType, args: List<State>) {
        val constructor = type.classOrNull!!.owner.constructors.first()
        val constructorCall = constructor.createConstructorCall()
        val constructorValueParameters = constructor.valueParameters.map { it.symbol }

        val primitiveValueParameters = args.map { it as Primitive<*> }
        primitiveValueParameters.forEachIndexed { index, primitive ->
            constructorCall.putValueArgument(index, primitive.value.toIrConst(constructorValueParameters[index].owner.type))
        }

        callStack.pushCompoundInstruction(constructorCall)
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
        callStack.pushCompoundInstruction(this)
        return body
    }
}
