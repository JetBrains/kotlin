/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.interpreter.exceptions.handleUserException
import org.jetbrains.kotlin.ir.interpreter.exceptions.verify
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

internal fun IrExpression.handleAndDropResult(callStack: CallStack, dropOnlyUnit: Boolean = false) {
    val dropResult = fun() {
        if (!dropOnlyUnit && !this.type.isUnit() || callStack.peekState().isUnit()) callStack.popState()
    }
    callStack.addInstruction(CustomInstruction(dropResult))
    callStack.addInstruction(CompoundInstruction(this))
}

internal fun unfoldInstruction(element: IrElement?, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    when (element) {
        null -> return
        is IrSimpleFunction -> unfoldFunction(element, environment)
        is IrConstructor -> unfoldConstructor(element, callStack)
        is IrCall -> unfoldValueParameters(element, environment)
        is IrConstructorCall -> unfoldValueParameters(element, environment)
        is IrEnumConstructorCall -> unfoldValueParameters(element, environment)
        is IrDelegatingConstructorCall -> unfoldValueParameters(element, environment)
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
        is IrGetClass -> unfoldGetClass(element, callStack)
        is IrComposite -> unfoldComposite(element, callStack)

        else -> TODO("${element.javaClass} not supported")
    }
}

private fun unfoldFunction(function: IrSimpleFunction, environment: IrInterpreterEnvironment) {
    if (environment.callStack.getStackCount() >= environment.configuration.maxStack) {
        environment.callStack.dropFrame() // current frame is pointing on function and is redundant
        return StackOverflowError().handleUserException(environment)
    }
    // SimpleInstruction with function is added in IrCall
    // It will serve as endpoint for all possible calls, there we drop frame and copy result to new one
    function.body?.let { environment.callStack.addInstruction(CompoundInstruction(it)) }
        ?: throw InterpreterError("Ir function must be with body")
}

private fun unfoldConstructor(constructor: IrConstructor, callStack: CallStack) {
    when (constructor.fqName) {
        "kotlin.Enum.<init>", "kotlin.Throwable.<init>" -> {
            val irClass = constructor.parentAsClass
            val receiverSymbol = irClass.thisReceiver!!.symbol
            val receiverState = callStack.getState(receiverSymbol)

            irClass.declarations.filterIsInstance<IrProperty>().forEach { property ->
                val parameter = constructor.valueParameters.singleOrNull { it.name == property.name }
                val state = parameter?.let { callStack.getState(it.symbol) } ?: Primitive.nullStateOfType(property.getter!!.returnType)
                receiverState.setField(Variable(property.symbol, state))
            }
        }
        else -> {
            // SimpleInstruction with function is added in constructor call
            // It will serve as endpoint for all possible constructor calls, there we drop frame and return object
            callStack.addInstruction(CompoundInstruction(constructor.body!!))
        }
    }
}

private fun unfoldValueParameters(expression: IrFunctionAccessExpression, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    val hasDefaults = (0 until expression.valueArgumentsCount).any { expression.getValueArgument(it) == null }
    if (hasDefaults) {
        val ownerWithDefaults = expression.getFunctionThatContainsDefaults()
        environment.createdFunctionsCache[ownerWithDefaults.symbol]?.let {
            val callToDefault = it.owner.createCall().apply { expression.copyArgsInto(this) }
            callStack.addInstruction(CompoundInstruction(callToDefault))
            return
        }

        // if some arguments are not defined, then it is necessary to create temp function where defaults will be evaluated
        val actualParameters = MutableList<IrValueDeclaration?>(expression.valueArgumentsCount) { null }
        val visibility = when (expression) {
            is IrEnumConstructorCall, is IrDelegatingConstructorCall -> DescriptorVisibilities.LOCAL
            else -> ownerWithDefaults.visibility
        }

        val defaultFun = createTempFunction(
            Name.identifier(ownerWithDefaults.name.asString() + "\$default"), ownerWithDefaults.returnType,
            origin = IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER, visibility
        ).apply {
            this.parent = ownerWithDefaults.parent
            this.dispatchReceiverParameter = ownerWithDefaults.dispatchReceiverParameter?.deepCopyWithSymbols(this)
            this.extensionReceiverParameter = ownerWithDefaults.extensionReceiverParameter?.deepCopyWithSymbols(this)
            (0 until expression.valueArgumentsCount).forEach { index ->
                val originalParameter = ownerWithDefaults.valueParameters[index]
                val copiedParameter = originalParameter.deepCopyWithSymbols(this)
                this.valueParameters += copiedParameter
                actualParameters[index] = if (copiedParameter.defaultValue != null) {
                    copiedParameter.type = copiedParameter.type.makeNullable() // make nullable type to keep consistency; parameter can be null if it is missing
                    val irGetParameter = copiedParameter.createGetValue()
                    val defaultInitializer = originalParameter.getDefaultWithActualParameters(this@apply, actualParameters)
                        ?: expression.getVarargType(index)?.let { null.toIrConst(it) } // if parameter is vararg and it is missing

                    copiedParameter.createTempVariable().apply variable@{
                        this@variable.initializer = environment.irBuiltIns.irIfNullThenElse(irGetParameter, defaultInitializer!!, irGetParameter)
                    }
                } else {
                    copiedParameter
                }
            }
        }

        val callWithAllArgs = expression.shallowCopy() // just a copy of given call, but with all arguments in place
        expression.dispatchReceiver?.let { callWithAllArgs.dispatchReceiver = defaultFun.dispatchReceiverParameter!!.createGetValue() }
        expression.extensionReceiver?.let { callWithAllArgs.extensionReceiver = defaultFun.extensionReceiverParameter!!.createGetValue() }
        (0 until expression.valueArgumentsCount).forEach { callWithAllArgs.putValueArgument(it, actualParameters[it]?.createGetValue()) }
        defaultFun.body = (actualParameters.filterIsInstance<IrVariable>() + defaultFun.createReturn(callWithAllArgs)).wrapWithBlockBody()

        environment.createdFunctionsCache[ownerWithDefaults.symbol] = defaultFun.symbol
        val callToDefault = defaultFun.createCall().apply { expression.copyArgsInto(this) }
        callStack.addInstruction(CompoundInstruction(callToDefault))
    } else {
        val irFunction = expression.symbol.owner
        callStack.addInstruction(SimpleInstruction(expression))

        fun IrValueParameter.schedule(arg: IrExpression?) {
            callStack.addInstruction(SimpleInstruction(this))
            callStack.addInstruction(CompoundInstruction(arg))
        }
        (expression.valueArgumentsCount - 1 downTo 0).forEach { irFunction.valueParameters[it].schedule(expression.getValueArgument(it)) }
        expression.extensionReceiver?.let { irFunction.extensionReceiverParameter!!.schedule(it) }
        expression.dispatchReceiver?.let { irFunction.dispatchReceiverParameter!!.schedule(it) }
    }
}

private fun unfoldInstanceInitializerCall(instanceInitializerCall: IrInstanceInitializerCall, callStack: CallStack) {
    val irClass = instanceInitializerCall.classSymbol.owner
    val toInitialize = irClass.declarations.filter { it is IrProperty || it is IrAnonymousInitializer }

    toInitialize.reversed().forEach {
        when {
            it is IrAnonymousInitializer -> callStack.addInstruction(CompoundInstruction(it.body))
            it is IrProperty && it.backingField?.initializer?.expression != null -> callStack.addInstruction(CompoundInstruction(it.backingField))
        }
    }
}

private fun unfoldField(field: IrField, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(field))
    callStack.addInstruction(CompoundInstruction(field.initializer?.expression))
}

private fun unfoldBody(body: IrBody, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(body))
    unfoldStatements(body.statements, callStack)
}

private fun unfoldBlock(block: IrBlock, callStack: CallStack) {
    callStack.newSubFrame(block)
    callStack.addInstruction(SimpleInstruction(block))
    unfoldStatements(block.statements, callStack)
}

private fun unfoldStatements(statements: List<IrStatement>, callStack: CallStack) {
    fun Int.isLastIndex(): Boolean = statements.size - 1 == this

    for (i in statements.indices.reversed()) {
        when (val statement = statements[i]) {
            is IrClass -> if (!statement.isLocal) TODO("Only local classes are supported")
            is IrFunction -> if (!statement.isLocal) TODO("Only local functions are supported")
            is IrExpression ->
                when {
                    i.isLastIndex() -> callStack.addInstruction(CompoundInstruction(statement))
                    else -> statement.handleAndDropResult(callStack, dropOnlyUnit = true)
                }
            else -> callStack.addInstruction(CompoundInstruction(statement))
        }
    }
}

private fun unfoldReturn(expression: IrReturn, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(expression))
    callStack.addInstruction(CompoundInstruction(expression.value))
}

private fun unfoldSetField(expression: IrSetField, callStack: CallStack) {
    verify(!expression.accessesTopLevelOrObjectField()) { "Cannot interpret set method on top level properties" }

    callStack.addInstruction(SimpleInstruction(expression))
    callStack.addInstruction(CompoundInstruction(expression.value))
}

private fun unfoldGetValue(expression: IrGetValue, environment: IrInterpreterEnvironment) {
    val expectedClass = expression.type.classOrNull?.owner
    // used to evaluate constants inside object
    if (expectedClass != null && expectedClass.isObject && expression.symbol.owner.origin == IrDeclarationOrigin.INSTANCE_RECEIVER) {
        // TODO is this correct behaviour?
        val irGetObject = expectedClass.createGetObject()
        return unfoldGetObjectValue(irGetObject, environment)
    }
    environment.callStack.pushState(environment.callStack.getState(expression.symbol))
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
    callStack.addInstruction(SimpleInstruction(branch))
    callStack.addInstruction(CompoundInstruction(branch.condition))
}

private fun unfoldWhileLoop(loop: IrWhileLoop, callStack: CallStack) {
    callStack.newSubFrame(loop)
    callStack.addInstruction(SimpleInstruction(loop))
    callStack.addInstruction(CompoundInstruction(loop.condition))
}

private fun unfoldDoWhileLoop(loop: IrDoWhileLoop, callStack: CallStack) {
    callStack.newSubFrame(loop)
    callStack.addInstruction(SimpleInstruction(loop))
    callStack.addInstruction(CompoundInstruction(loop.condition))
    callStack.addInstruction(CompoundInstruction(loop.body))
}

private fun unfoldWhen(element: IrWhen, callStack: CallStack) {
    // new sub frame to drop it after
    callStack.newSubFrame(element)
    callStack.addInstruction(SimpleInstruction(element))
    element.branches.reversed().forEach { callStack.addInstruction(CompoundInstruction(it)) }
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
    callStack.newSubFrame(element)
    callStack.addInstruction(SimpleInstruction(element))
    callStack.addInstruction(CompoundInstruction(element.tryResult))
}

private fun unfoldCatch(element: IrCatch, callStack: CallStack) {
    val exceptionState = callStack.peekState() as? ExceptionState ?: return
    if (exceptionState.isSubtypeOf(element.catchParameter.type)) {
        callStack.popState()
        val frameOwner = callStack.currentFrameOwner as IrTry
        callStack.dropSubFrame() // drop other catch blocks
        callStack.newSubFrame(element) // new frame with IrTry instruction to interpret finally block at the end
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
                val toStringCall = state.createToStringIrCall()
                callStack.addInstruction(SimpleInstruction(toStringCall))
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
        IrStatementOrigin.DESTRUCTURING_DECLARATION, IrStatementOrigin.DO_WHILE_LOOP, null -> // is null for body of do while loop
            element.statements.reversed().forEach { callStack.addInstruction(CompoundInstruction(it)) }
        else -> TODO("${element.origin} not implemented")
    }
}

private fun unfoldFunctionReference(reference: IrFunctionReference, callStack: CallStack) {
    val function = reference.symbol.owner
    callStack.addInstruction(SimpleInstruction(reference))

    reference.dispatchReceiver?.let { callStack.addInstruction(SimpleInstruction(function.dispatchReceiverParameter!!)) }
    reference.extensionReceiver?.let { callStack.addInstruction(SimpleInstruction(function.extensionReceiverParameter!!)) }

    reference.extensionReceiver?.let { callStack.addInstruction(CompoundInstruction(it)) }
    reference.dispatchReceiver?.let { callStack.addInstruction(CompoundInstruction(it)) }
}

private fun unfoldPropertyReference(propertyReference: IrPropertyReference, callStack: CallStack) {
    val getter = propertyReference.getter!!.owner
    callStack.addInstruction(SimpleInstruction(propertyReference))

    propertyReference.dispatchReceiver?.let { callStack.addInstruction(SimpleInstruction(getter.dispatchReceiverParameter!!)) }
    propertyReference.extensionReceiver?.let { callStack.addInstruction(SimpleInstruction(getter.extensionReceiverParameter!!)) }

    propertyReference.extensionReceiver?.let { callStack.addInstruction(CompoundInstruction(it)) }
    propertyReference.dispatchReceiver?.let { callStack.addInstruction(CompoundInstruction(it)) }
}

private fun unfoldClassReference(classReference: IrClassReference, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(classReference))
}

private fun unfoldGetClass(element: IrGetClass, callStack: CallStack) {
    callStack.addInstruction(SimpleInstruction(element))
    callStack.addInstruction(CompoundInstruction(element.argument))
}