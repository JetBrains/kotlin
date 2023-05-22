/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.interpreter.exceptions.handleUserException
import org.jetbrains.kotlin.ir.interpreter.exceptions.verify
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
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
    callStack.pushInstruction(CustomInstruction(dropResult))
    callStack.pushCompoundInstruction(this)
}

internal fun unfoldInstruction(element: IrElement?, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    when (element) {
        null -> return
        is IrSimpleFunction -> unfoldFunction(element, environment)
        is IrConstructor -> unfoldConstructor(element, callStack)
        is IrCall -> unfoldValueParameters(element, environment)
        is IrConstructorCall -> unfoldValueParameters(element, environment)
        is IrEnumConstructorCall -> unfoldEnumConstructorCall(element, environment)
        is IrDelegatingConstructorCall -> unfoldValueParameters(element, environment)
        is IrInstanceInitializerCall -> unfoldInstanceInitializerCall(element, callStack)
        is IrField -> unfoldField(element, callStack)
        is IrBody -> unfoldBody(element, callStack)
        is IrBlock -> unfoldBlock(element, callStack)
        is IrReturn -> unfoldReturn(element, callStack)
        is IrSetField -> unfoldSetField(element, callStack)
        is IrGetField -> callStack.pushSimpleInstruction(element)
        is IrGetValue -> unfoldGetValue(element, environment)
        is IrGetObjectValue -> unfoldGetObjectValue(element, environment)
        is IrGetEnumValue -> unfoldGetEnumValue(element, environment)
        is IrEnumEntry -> unfoldEnumEntry(element, environment)
        is IrConst<*> -> callStack.pushSimpleInstruction(element)
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
        is IrSpreadElement -> callStack.pushCompoundInstruction(element.expression)
        is IrTry -> unfoldTry(element, callStack)
        is IrCatch -> unfoldCatch(element, callStack)
        is IrThrow -> unfoldThrow(element, callStack)
        is IrStringConcatenation -> unfoldStringConcatenation(element, environment)
        is IrFunctionExpression -> callStack.pushSimpleInstruction(element)
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
    function.body?.let { environment.callStack.pushCompoundInstruction(it) }
        ?: throw InterpreterError("Ir function must be with body")
}

private fun unfoldConstructor(constructor: IrConstructor, callStack: CallStack) {
    when (constructor.fqName) {
        "kotlin.Enum.<init>" -> return // all properties already initialized in `interpretEnumEntry`
        "kotlin.Throwable.<init>" -> {
            val irClass = constructor.parentAsClass
            val receiverSymbol = irClass.thisReceiver!!.symbol
            val receiverState = callStack.loadState(receiverSymbol)

            irClass.declarations.filterIsInstance<IrProperty>().forEach { property ->
                val parameter = constructor.valueParameters.singleOrNull { it.name == property.name }
                val state = parameter?.let { callStack.loadState(it.symbol) } ?: Primitive.nullStateOfType(property.getter!!.returnType)
                receiverState.setField(property.symbol, state)
            }
        }
        else -> {
            // SimpleInstruction with function is added in constructor call
            // It will serve as endpoint for all possible constructor calls, there we drop frame and return object
            callStack.pushCompoundInstruction(constructor.body!!)
        }
    }
}

private fun unfoldValueParameters(expression: IrFunctionAccessExpression, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    val irFunction = expression.symbol.owner

    if (irFunction.name.asString() == "<get-name>" && expression.dispatchReceiver is IrGetEnumValue) {
        // this optimization allow us to avoid creation of enum object when we try to interpret simple `name` call; see KT-53480
        callStack.pushSimpleInstruction(expression)
        callStack.pushSimpleInstruction(irFunction.dispatchReceiverParameter!!)

        val enumEntry = (expression.dispatchReceiver as IrGetEnumValue).symbol.owner
        callStack.pushState(enumEntry.toState(environment.irBuiltIns))
        return
    }

    val hasDefaults = (0 until expression.valueArgumentsCount).any { expression.getValueArgument(it) == null }
    if (hasDefaults) {
        environment.getCachedFunction(expression.symbol, fromDelegatingCall = expression is IrDelegatingConstructorCall)?.let {
            val callToDefault = it.owner.createCall().apply { environment.irBuiltIns.copyArgs(expression, this) }
            callStack.pushCompoundInstruction(callToDefault)
            return
        }

        // if some arguments are not defined, then it is necessary to create temp function where defaults will be evaluated
        val actualParameters = MutableList<IrValueDeclaration?>(expression.valueArgumentsCount) { null }
        val ownerWithDefaults = expression.getFunctionThatContainsDefaults()
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
                actualParameters[index] = if (copiedParameter.defaultValue != null || copiedParameter.isVararg) {
                    copiedParameter.type = copiedParameter.type.makeNullable() // make nullable type to keep consistency; parameter can be null if it is missing
                    val irGetParameter = copiedParameter.createGetValue()
                    // if parameter is vararg and it is missing, then create constructor call for empty array
                    val defaultInitializer = originalParameter.getDefaultWithActualParameters(this@apply, actualParameters)
                        ?: environment.irBuiltIns.emptyArrayConstructor(expression.getVarargType(index)!!.getTypeIfReified(callStack))

                    copiedParameter.createTempVariable().apply variable@{
                        this@variable.initializer = environment.irBuiltIns.irIfNullThenElse(irGetParameter, defaultInitializer, irGetParameter)
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

        val callToDefault = environment.setCachedFunction(
            expression.symbol, fromDelegatingCall = expression is IrDelegatingConstructorCall, newFunction = defaultFun.symbol
        ).owner.createCall().apply { environment.irBuiltIns.copyArgs(expression, this) }
        callStack.pushCompoundInstruction(callToDefault)
    } else {
        callStack.pushSimpleInstruction(expression)

        fun IrValueParameter.schedule(arg: IrExpression?) {
            callStack.pushSimpleInstruction(this)
            callStack.pushCompoundInstruction(arg)
        }
        (expression.valueArgumentsCount - 1 downTo 0).forEach { irFunction.valueParameters[it].schedule(expression.getValueArgument(it)) }
        expression.extensionReceiver?.let { irFunction.extensionReceiverParameter!!.schedule(it) }
        expression.dispatchReceiver?.let { irFunction.dispatchReceiverParameter!!.schedule(it) }
    }
}

private fun unfoldEnumConstructorCall(element: IrEnumConstructorCall, environment: IrInterpreterEnvironment) {
    val parentName = element.symbol.owner.parentClassOrNull?.fqName
    if (parentName == "kotlin.Enum") {
        // must create a copy here to avoid original data corruption
        val constructorCallCopy = element.shallowCopy()
        val enumObject = environment.callStack.loadState(element.getThisReceiver())
        environment.irBuiltIns.enumClass.owner.declarations.filterIsInstance<IrProperty>().forEachIndexed { index, it ->
            val field = enumObject.getField(it.symbol) as Primitive<*>
            constructorCallCopy.putValueArgument(index, field.value.toIrConst(field.type))
        }
        return unfoldValueParameters(constructorCallCopy, environment)
    }
    unfoldValueParameters(element, environment)
}

private fun unfoldInstanceInitializerCall(instanceInitializerCall: IrInstanceInitializerCall, callStack: CallStack) {
    val irClass = instanceInitializerCall.classSymbol.owner
    val toInitialize = irClass.declarations.filter { it is IrProperty || it is IrAnonymousInitializer }
    val state = irClass.thisReceiver?.symbol?.let { callStack.loadState(it) } // try to avoid recalculation of properties

    toInitialize.reversed().forEach {
        when {
            it is IrAnonymousInitializer -> callStack.pushCompoundInstruction(it.body)

            it is IrProperty && it.backingField?.initializer?.expression != null && state?.getField(it.symbol) == null -> {
                callStack.pushCompoundInstruction(it.backingField)
            }
        }
    }
}

private fun unfoldField(field: IrField, callStack: CallStack) {
    callStack.pushSimpleInstruction(field)
    callStack.pushCompoundInstruction(field.initializer?.expression)
}

private fun unfoldBody(body: IrBody, callStack: CallStack) {
    callStack.pushSimpleInstruction(body)
    unfoldStatements(body.statements, callStack)
}

private fun unfoldBlock(block: IrBlock, callStack: CallStack) {
    callStack.newSubFrame(block)
    callStack.pushSimpleInstruction(block)
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
                    i.isLastIndex() -> callStack.pushCompoundInstruction(statement)
                    else -> statement.handleAndDropResult(callStack, dropOnlyUnit = true)
                }
            else -> callStack.pushCompoundInstruction(statement)
        }
    }
}

private fun unfoldReturn(expression: IrReturn, callStack: CallStack) {
    callStack.pushSimpleInstruction(expression)
    callStack.pushCompoundInstruction(expression.value)
}

private fun unfoldSetField(expression: IrSetField, callStack: CallStack) {
    verify(!expression.accessesTopLevelOrObjectField()) { "Cannot interpret set method on top level properties" }

    callStack.pushSimpleInstruction(expression)
    callStack.pushCompoundInstruction(expression.value)
}

private fun unfoldGetValue(expression: IrGetValue, environment: IrInterpreterEnvironment) {
    if (expression.isAccessToNotNullableObject()) {
        // used to evaluate constants inside object
        // TODO is this correct behaviour?
        val irGetObject = expression.type.classOrNull?.owner!!.createGetObject()
        return unfoldGetObjectValue(irGetObject, environment) // if object already exists, it will be taken from `mapOfObjects`
    }
    environment.callStack.pushState(environment.callStack.loadState(expression.symbol))
}

private fun unfoldGetObjectValue(expression: IrGetObjectValue, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    val objectClass = expression.symbol.owner
    environment.mapOfObjects[objectClass.symbol]?.let { return callStack.pushState(it) }

    callStack.pushSimpleInstruction(expression)
}

private fun unfoldGetEnumValue(expression: IrGetEnumValue, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    environment.mapOfEnums[expression.symbol]?.let { return callStack.pushState(it) }

    callStack.pushSimpleInstruction(expression)
    val enumEntry = expression.symbol.owner
    val enumClass = enumEntry.symbol.owner.parentAsClass
    enumClass.declarations.filterIsInstance<IrEnumEntry>().reversed().forEach {
        callStack.pushSimpleInstruction(it)
    }
}

@Suppress("UNUSED_PARAMETER")
private fun unfoldEnumEntry(enumEntry: IrEnumEntry, environment: IrInterpreterEnvironment) {
    // a little hak and misconception here; we are not creating simple instructions from this and just do the cleaning after interpretation
    environment.callStack.popState()
    environment.callStack.dropSubFrame()
}

private fun unfoldVariable(variable: IrVariable, callStack: CallStack) {
    when (variable.initializer) {
        null -> callStack.storeState(variable.symbol, null)
        else -> {
            callStack.pushSimpleInstruction(variable)
            callStack.pushCompoundInstruction(variable.initializer!!)
        }
    }
}

private fun unfoldSetValue(expression: IrSetValue, callStack: CallStack) {
    callStack.pushSimpleInstruction(expression)
    callStack.pushCompoundInstruction(expression.value)
}

private fun unfoldTypeOperatorCall(element: IrTypeOperatorCall, callStack: CallStack) {
    callStack.pushSimpleInstruction(element)
    callStack.pushCompoundInstruction(element.argument)
}

private fun unfoldBranch(branch: IrBranch, callStack: CallStack) {
    callStack.pushSimpleInstruction(branch)
    callStack.pushCompoundInstruction(branch.condition)
}

private fun unfoldWhileLoop(loop: IrWhileLoop, callStack: CallStack) {
    callStack.newSubFrame(loop)
    callStack.pushSimpleInstruction(loop)
    callStack.pushCompoundInstruction(loop.condition)
}

private fun unfoldDoWhileLoop(loop: IrDoWhileLoop, callStack: CallStack) {
    callStack.newSubFrame(loop)
    callStack.pushSimpleInstruction(loop)
    callStack.pushCompoundInstruction(loop.condition)
    callStack.pushCompoundInstruction(loop.body)
}

private fun unfoldWhen(element: IrWhen, callStack: CallStack) {
    // new sub frame to drop it after
    callStack.newSubFrame(element)
    callStack.pushSimpleInstruction(element)
    element.branches.reversed().forEach { callStack.pushCompoundInstruction(it) }
}

private fun unfoldContinue(element: IrContinue, callStack: CallStack) {
    callStack.unrollInstructionsForBreakContinue(element)
}

private fun unfoldBreak(element: IrBreak, callStack: CallStack) {
    callStack.unrollInstructionsForBreakContinue(element)
}

private fun unfoldVararg(element: IrVararg, callStack: CallStack) {
    callStack.pushSimpleInstruction(element)
    element.elements.reversed().forEach { callStack.pushCompoundInstruction(it) }
}

private fun unfoldTry(element: IrTry, callStack: CallStack) {
    callStack.newSubFrame(element)
    callStack.pushSimpleInstruction(element)
    callStack.pushCompoundInstruction(element.tryResult)
}

private fun unfoldCatch(element: IrCatch, callStack: CallStack) {
    val exceptionState = callStack.peekState() as? ExceptionState ?: return
    if (exceptionState.isSubtypeOf(element.catchParameter.type)) {
        callStack.popState()
        val frameOwner = callStack.currentFrameOwner as IrTry
        callStack.dropSubFrame() // drop other catch blocks
        callStack.newSubFrame(element) // new frame with IrTry instruction to interpret finally block at the end
        callStack.storeState(element.catchParameter.symbol, exceptionState)
        callStack.pushSimpleInstruction(frameOwner)
        callStack.pushCompoundInstruction(element.result)
    }
}

private fun unfoldThrow(expression: IrThrow, callStack: CallStack) {
    callStack.pushSimpleInstruction(expression)
    callStack.pushCompoundInstruction(expression.value)
}

private fun unfoldStringConcatenation(expression: IrStringConcatenation, environment: IrInterpreterEnvironment) {
    val callStack = environment.callStack
    callStack.pushSimpleInstruction(expression)

    // this callback is used to check the need for an explicit toString call
    val explicitToStringCheck = fun() {
        when (val state = callStack.peekState()) {
            is Primitive<*> -> {
                // This block is not really needed, but this way it is easier to handle `toString` for JS.
                callStack.popState()
                val toStringCall = IrCallImpl.fromSymbolOwner(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    if (state.isNull()) environment.irBuiltIns.extensionToString else environment.irBuiltIns.memberToString
                )
                callStack.pushSimpleInstruction(toStringCall)
                callStack.pushState(state)
            }
            is Common -> {
                callStack.popState()
                // TODO this check can be dropped after serialization introduction
                // for now declarations in unsigned class don't have bodies and must be treated separately
                if (state.irClass.defaultType.isUnsigned()) {
                    return callStack.pushState(environment.convertToState(state.unsignedToString(), environment.irBuiltIns.stringType))
                }
                val toStringCall = state.createToStringIrCall()
                callStack.pushSimpleInstruction(toStringCall)
                callStack.pushState(state)
            }
        }
    }
    expression.arguments.reversed().forEach {
        callStack.pushInstruction(CustomInstruction(explicitToStringCheck))
        callStack.pushCompoundInstruction(it)
    }
}

private fun unfoldComposite(element: IrComposite, callStack: CallStack) {
    when (element.origin) {
        IrStatementOrigin.DESTRUCTURING_DECLARATION, IrStatementOrigin.DO_WHILE_LOOP, null -> // is null for body of do while loop
            element.statements.reversed().forEach { callStack.pushCompoundInstruction(it) }
        else -> TODO("${element.origin} not implemented")
    }
}

private fun unfoldCallableReference(reference: IrCallableReference<*>, callStack: CallStack) {
    callStack.pushSimpleInstruction(reference)
    reference.getArgumentsWithIr().forEach { (parameter, arg) ->
        callStack.pushSimpleInstruction(parameter)
        callStack.pushCompoundInstruction(arg)
    }
}

private fun unfoldFunctionReference(reference: IrFunctionReference, callStack: CallStack) {
    unfoldCallableReference(reference, callStack)
}

private fun unfoldPropertyReference(propertyReference: IrPropertyReference, callStack: CallStack) {
    if (propertyReference.field != null) return callStack.pushSimpleInstruction(propertyReference)
    unfoldCallableReference(propertyReference, callStack)
}

private fun unfoldClassReference(classReference: IrClassReference, callStack: CallStack) {
    callStack.pushSimpleInstruction(classReference)
}

private fun unfoldGetClass(element: IrGetClass, callStack: CallStack) {
    callStack.pushSimpleInstruction(element)
    callStack.pushCompoundInstruction(element.argument)
}