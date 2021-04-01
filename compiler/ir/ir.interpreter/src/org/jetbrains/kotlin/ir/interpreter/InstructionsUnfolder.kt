/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*

internal fun unfoldInstruction(element: IrElement?, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    when (element) {
        null -> return
        is IrSimpleFunction -> unfoldFunction(element, environment)
        is IrConstructor -> unfoldConstructor(element, callStack)
        is IrCall -> unfoldCall(element, callStack)
        is IrConstructorCall -> unfoldConstructorCall(element, callStack)
        is IrEnumConstructorCall -> unfoldEnumConstructorCall(element, callStack)
        is IrDelegatingConstructorCall -> unfoldDelegatingConstructorCall(element, callStack)
        is IrInstanceInitializerCall -> unfoldInstanceInitializerCall(element, callStack)
        is IrField -> unfoldField(element, callStack)
        is IrBody -> unfoldBody(element, callStack)
        is IrBlock -> unfoldBlock(element, callStack)
        is IrReturn -> unfoldReturn(element, callStack)
        is IrSetField -> unfoldSetField(element, callStack)
        is IrGetField -> callStack.addInstruction(SimpleInstruction(element))
        is IrGetValue -> unfoldGetValue(element, environment)
        is IrGetObjectValue -> unfoldGetObjectValue(element, environment)
        is IrGetEnumValue -> unfoldGetEnumValue(element, environment)
        is IrConst<*> -> callStack.addInstruction(SimpleInstruction(element))
        is IrVariable -> unfoldVariable(element, callStack)
        is IrSetValue -> unfoldSetValue(element, callStack)
        is IrTypeOperatorCall -> unfoldTypeOperatorCall(element, callStack)
        is IrBranch -> unfoldBranch(element, callStack)
        is IrWhileLoop -> unfoldWhileLoop(element, callStack)
        is IrDoWhileLoop -> unfoldDoWhileLoop(element, callStack)
        is IrWhen -> unfoldWhen(element, callStack)
        is IrBreak -> unfoldBreak(element, callStack)
        is IrContinue -> unfoldContinue(element, callStack)
        is IrVararg -> unfoldVararg(element, callStack)
        is IrSpreadElement -> callStack.addInstruction(CompoundInstruction(element.expression))
        is IrTry -> unfoldTry(element, callStack)
        is IrCatch -> unfoldCatch(element, callStack)
        is IrThrow -> unfoldThrow(element, callStack)
        is IrStringConcatenation -> unfoldStringConcatenation(element, environment)
        is IrFunctionExpression -> callStack.addInstruction(SimpleInstruction(element))
        is IrFunctionReference -> unfoldFunctionReference(element, callStack)
        is IrPropertyReference -> unfoldPropertyReference(element, callStack)
        is IrClassReference -> unfoldClassReference(element, callStack)
        is IrComposite -> unfoldComposite(element, callStack)

        else -> TODO("${element.javaClass} not supported")
    }
}

private fun unfoldFunction(function: IrSimpleFunction, environment: IrInterpreterEnvironment) {
    if (environment.callStack.getStackCount() >= IrInterpreterEnvironment.MAX_STACK)
        return StackOverflowError().handleUserException(environment)
    // SimpleInstruction with function is added in IrCall
    // It will serve as endpoint for all possible calls, there we drop frame and copy result to new one
    function.body?.let { environment.callStack.addInstruction(CompoundInstruction(it)) }
        ?: throw InterpreterError("Ir function must be with body")
}

private fun unfoldConstructor(constructor: IrConstructor, callStack: CallStack) {
    // SimpleInstruction with function is added in constructor call
    // It will serve as endpoint for all possible constructor calls, there we drop frame and return object
    callStack.addInstruction(CompoundInstruction(constructor.body!!))
}

private fun unfoldCall(call: IrCall, callStack: CallStack) {
    val function = call.symbol.owner
    // new sub frame is used to store value arguments, in case then they are used in default args evaluation
    callStack.newSubFrame(call, listOf())
    callStack.addInstruction(SimpleInstruction(call))
    unfoldValueParameters(call, callStack)

    // must save receivers in memory in case then they are used in default args evaluation
    call.extensionReceiver?.let {
        callStack.addInstruction(SimpleInstruction(function.extensionReceiverParameter!!))
        callStack.addInstruction(CompoundInstruction(it))
    }
    call.dispatchReceiver?.let {
        callStack.addInstruction(SimpleInstruction(function.dispatchReceiverParameter!!))
        callStack.addInstruction(CompoundInstruction(it))
    }
}

private fun unfoldConstructorCall(constructorCall: IrFunctionAccessExpression, callStack: CallStack) {
    val constructor = constructorCall.symbol.owner
    callStack.newSubFrame(constructorCall, listOf()) // used to store value arguments, in case then they are use as default args
    // this variable is used to create object once
    callStack.addVariable(Variable(constructorCall.getThisReceiver(), Common(constructor.parentAsClass)))
    callStack.addInstruction(SimpleInstruction(constructorCall))
    unfoldValueParameters(constructorCall, callStack)

    constructorCall.dispatchReceiver?.let {
        callStack.addInstruction(SimpleInstruction(constructor.dispatchReceiverParameter!!))
        callStack.addInstruction(CompoundInstruction(it))
    }
}

private fun unfoldEnumConstructorCall(enumConstructorCall: IrEnumConstructorCall, callStack: CallStack) {
    callStack.newSubFrame(enumConstructorCall, listOf()) // used to store value arguments, in case then they are use as default args
    callStack.addInstruction(SimpleInstruction(enumConstructorCall))
    unfoldValueParameters(enumConstructorCall, callStack)
}

private fun unfoldDelegatingConstructorCall(delegatingConstructorCall: IrFunctionAccessExpression, callStack: CallStack) {
    callStack.newSubFrame(delegatingConstructorCall, listOf()) // used to store value arguments, in case then they are use as default args
    callStack.addInstruction(SimpleInstruction(delegatingConstructorCall))
    unfoldValueParameters(delegatingConstructorCall, callStack)
}

private fun unfoldValueParameters(expression: IrFunctionAccessExpression, callStack: CallStack) {
    fun IrValueParameter.getDefault(): IrExpressionBody? {
        return defaultValue
            ?: (this.parent as? IrSimpleFunction)?.overriddenSymbols
                ?.map { it.owner.valueParameters[this.index].getDefault() }
                ?.firstNotNullOfOrNull { it }
    }

    fun getDefaultForParameterAt(index: Int): IrExpression? {
        return expression.symbol.owner.valueParameters[index].getDefault()?.expression
    }

    val irFunction = expression.symbol.owner
    val valueParametersSymbols = irFunction.valueParameters.map { it.symbol }
    val valueArguments = (0 until expression.valueArgumentsCount).map { expression.getValueArgument(it) }

    for (i in valueParametersSymbols.indices.reversed()) {
        callStack.addInstruction(SimpleInstruction(valueParametersSymbols[i].owner))
        val arg = valueArguments[i] ?: getDefaultForParameterAt(i)
        when {
            arg != null -> callStack.addInstruction(CompoundInstruction(arg))
            else ->
                // case when value parameter is vararg and it is missing
                expression.getVarargType(i)?.let {
                    callStack.addInstruction(SimpleInstruction(IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it)))
                }
        }
    }

    // hack for extension receiver in lambda
    if (expression.valueArgumentsCount != irFunction.valueParameters.size) {
        val extensionReceiver = irFunction.getExtensionReceiver() ?: return
        callStack.addInstruction(SimpleInstruction(extensionReceiver.owner))
        valueArguments[0]?.let { callStack.addInstruction(CompoundInstruction(it)) }
    }
}

private fun unfoldInstanceInitializerCall(instanceInitializerCall: IrInstanceInitializerCall, callStack: CallStack) {
    val irClass = instanceInitializerCall.classSymbol.owner

    // init blocks processing
    val anonymousInitializer = irClass.declarations.filterIsInstance<IrAnonymousInitializer>().filter { !it.isStatic }
    anonymousInitializer.reversed().forEach { callStack.addInstruction(CompoundInstruction(it.body)) }

    // properties processing
    val classProperties = irClass.declarations.filterIsInstance<IrProperty>()
    classProperties.filter { it.backingField?.initializer?.expression != null }.reversed()
        .forEach { callStack.addInstruction(CompoundInstruction(it.backingField)) }
}

private fun unfoldField(field: IrField, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(field))
    callStack.addInstruction(CompoundInstruction(field.initializer?.expression))
}

private fun unfoldBody(body: IrBody, callStack: CallStack) {
    unfoldStatements(body.statements, callStack)
}

private fun unfoldBlock(block: IrBlock, callStack: CallStack) {
    callStack.newSubFrame(block, listOf())
    callStack.addInstruction(SimpleInstruction(block))
    unfoldStatements(block.statements, callStack)
}

private fun unfoldStatements(statements: List<IrStatement>, callStack: CallStack) {
    for (i in statements.indices.reversed()) {
        when (val statement = statements[i]) {
            is IrClass -> if (!statement.isLocal) TODO("Only local classes are supported")
            is IrFunction -> if (!statement.isLocal) TODO("Only local functions are supported")
            else -> callStack.addInstruction(CompoundInstruction(statement))
        }
    }
}

private fun unfoldReturn(expression: IrReturn, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(expression)) //2
    callStack.addInstruction(CompoundInstruction(expression.value)) //1
}

private fun unfoldSetField(expression: IrSetField, callStack: CallStack) {
    // receiver is null, for example, for top level fields; cannot interpret set on top level var
    if (expression.receiver.let { it == null || (it.type.classifierOrNull?.owner as? IrClass)?.isObject == true }) {
        error("Cannot interpret set method on top level properties")
    }

    callStack.addInstruction(SimpleInstruction(expression))
    callStack.addInstruction(CompoundInstruction(expression.value))
}

private fun unfoldGetValue(expression: IrGetValue, environment: IrInterpreterEnvironment) {
    val expectedClass = expression.type.classOrNull?.owner
    // used to evaluate constants inside object
    if (expectedClass != null && expectedClass.isObject && expression.symbol.owner.origin == IrDeclarationOrigin.INSTANCE_RECEIVER) {
        // TODO is this correct behaviour?
        return unfoldGetObjectValue(IrGetObjectValueImpl(0, 0, expectedClass.defaultType, expectedClass.symbol), environment)
    }
    environment.callStack.pushState(environment.callStack.getVariable(expression.symbol).state)
}

private fun unfoldGetObjectValue(expression: IrGetObjectValue, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    val objectClass = expression.symbol.owner
    environment.mapOfObjects[objectClass.symbol]?.let { return callStack.pushState(it) }

    callStack.addInstruction(SimpleInstruction(expression))
}

private fun unfoldGetEnumValue(expression: IrGetEnumValue, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    environment.mapOfEnums[expression.symbol]?.let { return callStack.pushState(it) }

    callStack.addInstruction(SimpleInstruction(expression))
    val enumEntry = expression.symbol.owner
    val enumClass = enumEntry.symbol.owner.parentAsClass
    enumClass.declarations.filterIsInstance<IrEnumEntry>().forEach {
        callStack.addInstruction(SimpleInstruction(it))
    }
}

private fun unfoldVariable(variable: IrVariable, callStack: CallStack) {
    when (variable.initializer) {
        null -> callStack.addVariable(Variable(variable.symbol))
        else -> {
            callStack.addInstruction(SimpleInstruction(variable))
            callStack.addInstruction(CompoundInstruction(variable.initializer!!))
        }
    }
}

private fun unfoldSetValue(expression: IrSetValue, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(expression))
    callStack.addInstruction(CompoundInstruction(expression.value))
}

private fun unfoldTypeOperatorCall(element: IrTypeOperatorCall, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(element))
    callStack.addInstruction(CompoundInstruction(element.argument))
}

private fun unfoldBranch(branch: IrBranch, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(branch)) //2
    callStack.addInstruction(CompoundInstruction(branch.condition)) //1
}

private fun unfoldWhileLoop(loop: IrWhileLoop, callStack: CallStack) {
    callStack.newSubFrame(loop, listOf())
    callStack.addInstruction(SimpleInstruction(loop))
    callStack.addInstruction(CompoundInstruction(loop.condition))
}

private fun unfoldDoWhileLoop(loop: IrDoWhileLoop, callStack: CallStack) {
    callStack.newSubFrame(loop, listOf())
    callStack.addInstruction(SimpleInstruction(loop))
    callStack.addInstruction(CompoundInstruction(loop.condition))
    callStack.addInstruction(CompoundInstruction(loop.body))
}

private fun unfoldWhen(element: IrWhen, callStack: CallStack) {
    // new sub frame to drop it after
    callStack.newSubFrame(element, element.branches.map { CompoundInstruction(it) } + listOf(SimpleInstruction(element)))
}

private fun unfoldContinue(element: IrContinue, callStack: CallStack) {
    callStack.unrollInstructionsForBreakContinue(element)
}

private fun unfoldBreak(element: IrBreak, callStack: CallStack) {
    callStack.unrollInstructionsForBreakContinue(element)
}

private fun unfoldVararg(element: IrVararg, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(element))
    element.elements.reversed().forEach { callStack.addInstruction(CompoundInstruction(it)) }
}

private fun unfoldTry(element: IrTry, callStack: CallStack) {
    callStack.newSubFrame(element, listOf())
    callStack.addInstruction(SimpleInstruction(element))
    callStack.addInstruction(CompoundInstruction(element.tryResult))
}

private fun unfoldCatch(element: IrCatch, callStack: CallStack) {
    val exceptionState = callStack.peekState() as? ExceptionState ?: return
    if (exceptionState.isSubtypeOf(element.catchParameter.type)) {
        callStack.popState()
        val frameOwner = callStack.getCurrentFrameOwner() as IrTry
        callStack.dropSubFrame() // drop other catch blocks
        callStack.newSubFrame(element, listOf()) // new frame with IrTry instruction to interpret finally block at the end
        callStack.addVariable(Variable(element.catchParameter.symbol, exceptionState))
        callStack.addInstruction(SimpleInstruction(frameOwner))
        callStack.addInstruction(CompoundInstruction(element.result))
    }
}

private fun unfoldThrow(expression: IrThrow, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(expression))
    callStack.addInstruction(CompoundInstruction(expression.value))
}

private fun unfoldStringConcatenation(expression: IrStringConcatenation, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    callStack.newSubFrame(expression, listOf())
    callStack.addInstruction(SimpleInstruction(expression))

    // this callback is used to check the need for an explicit toString call
    val explicitToStringCheck = fun() {
        when (val state = callStack.peekState()) {
            is Common -> {
                callStack.popState()
                // TODO this check can be dropped after serialization introduction
                // for now declarations in unsigned class don't have bodies and must be treated separately
                if (state.irClass.defaultType.isUnsigned()) {
                    val result = when (val value = (state.fields.single().state as Primitive<*>).value) {
                        is Byte -> value.toUByte().toString()
                        is Short -> value.toUShort().toString()
                        is Int -> value.toUInt().toString()
                        else -> (value as Number).toLong().toULong().toString()
                    }
                    return callStack.pushState(result.toState(environment.irBuiltIns.stringType))
                }
                val toStringFun = state.getToStringFunction()
                val receiver = toStringFun.dispatchReceiverParameter!!
                val toStringCall = IrCallImpl.fromSymbolOwner(0, 0, environment.irBuiltIns.stringType, toStringFun.symbol)
                toStringCall.dispatchReceiver = IrConstImpl.constNull(0, 0, receiver.type) // just stub receiver

                callStack.newSubFrame(toStringCall, listOf(SimpleInstruction(toStringCall)))
                callStack.pushState(state)
            }
        }
    }
    expression.arguments.reversed().forEach {
        callStack.addInstruction(CustomInstruction(explicitToStringCheck))
        callStack.addInstruction(CompoundInstruction(it))
    }
}

private fun unfoldComposite(element: IrComposite, callStack: CallStack) {
    when (element.origin) {
        IrStatementOrigin.DESTRUCTURING_DECLARATION -> element.statements.reversed()
            .forEach { callStack.addInstruction(CompoundInstruction(it)) }
        null -> element.statements.reversed()
            .forEach { callStack.addInstruction(CompoundInstruction(it)) } // is null for body of do while loop
        else -> TODO("${element.origin} not implemented")
    }
}

private fun unfoldFunctionReference(reference: IrFunctionReference, callStack: CallStack) {
    val function = reference.symbol.owner
    callStack.newSubFrame(reference, listOf())
    callStack.addInstruction(SimpleInstruction(reference))

    reference.extensionReceiver?.let {
        callStack.addInstruction(SimpleInstruction(function.extensionReceiverParameter!!))
        callStack.addInstruction(CompoundInstruction(it))
    }
    reference.dispatchReceiver?.let {
        callStack.addInstruction(SimpleInstruction(function.dispatchReceiverParameter!!))
        callStack.addInstruction(CompoundInstruction(it))
    }
}

private fun unfoldPropertyReference(propertyReference: IrPropertyReference, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(propertyReference))
    callStack.addInstruction(CompoundInstruction(propertyReference.dispatchReceiver))
}

private fun unfoldClassReference(classReference: IrClassReference, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(classReference))
}