/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.builtins.CompileTimeFunction
import org.jetbrains.kotlin.backend.common.interpreter.builtins.binaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.builtins.unaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.types.TypeUtils

enum class Code(var info: String = "") {
    NEXT, RETURN, BREAK_LOOP, BREAK_WHEN, CONTINUE, EXCEPTION
}

fun interpret(expression: IrExpression): IrExpression {
    return InterpreterFrame().apply { expression.interpret(this) }.popReturnValue().toIrExpression(expression)
}

fun IrElement.interpret(data: Frame): Code {
    try {
        val code = when (this) {
            is IrCall -> this.interpretCall(data)
            is IrConstructorCall -> this.interpretConstructorCall(data)
            is IrDelegatingConstructorCall -> this.interpretDelegatedConstructorCall(data)
            is IrBody -> this.interpretBody(data)
            is IrBlock -> this.interpretBlock(data)
            is IrReturn -> this.interpretReturn(data)
            is IrSetField -> this.interpretSetField(data)
            is IrGetField -> this.interpretGetField(data)
            is IrGetValue -> this.interpretGetValue(data)
            is IrGetObjectValue -> this.interpretGetObjectValue(data)
            is IrConst<*> -> this.interpretConst(data)
            is IrVariable -> this.interpretVariable(data)
            is IrSetVariable -> this.interpretSetVariable(data)
            is IrTypeOperatorCall -> this.interpretTypeOperatorCall(data)
            is IrBranch -> this.interpretBranch(data)
            is IrWhileLoop -> this.interpretWhile(data)
            is IrWhen -> this.interpretWhen(data)
            is IrBreak -> this.interpretBreak(data)

            else -> TODO("${this.javaClass} not supported")
        }

        return when (code) {
            Code.RETURN -> when (this) {
                is IrCall -> Code.NEXT
                else -> Code.RETURN
            }
            Code.BREAK_WHEN -> when (this) {
                is IrWhen -> Code.NEXT
                else -> code
            }
            Code.BREAK_LOOP -> when (this) {
                is IrWhileLoop -> if ((this.label ?: "") == code.info) Code.NEXT else code
                else -> code
            }
            Code.CONTINUE -> TODO("Code.CONTINUE not implemented")
            Code.EXCEPTION -> TODO("Code.EXCEPTION not implemented")
            Code.NEXT -> Code.NEXT
        }
    } catch (e: Exception) {
        e.printStackTrace()
        assert(false)
        return Code.EXCEPTION
    }
}

private fun calculateAbstract(irFunction: IrFunction, data: Frame): Code {
    return irFunction.body?.interpret(data)
        ?: throw NoSuchMethodException("Method \"${irFunction.name}\" wasn't implemented")
}

private fun calculateOverridden(owner: IrFunctionImpl, data: Frame): Code {
    val variableDescriptor = owner.symbol.getThisAsReceiver()!!
    val superQualifier = (data.getVariableState(variableDescriptor) as Complex).getSuperQualifier()!!
    val overridden = owner.overriddenSymbols.first { it.getThisAsReceiver()?.equalTo(superQualifier.getThisReceiver()) == true }

    val valueParameters = owner.valueParameters.zip(overridden.owner.valueParameters)
        .map { Variable(it.second.descriptor, data.getVariableState(it.first.descriptor)) }
    val newStates = InterpreterFrame((valueParameters + Variable(superQualifier.getThisReceiver(), superQualifier)).toMutableList())

    val overriddenOwner = overridden.owner as IrFunctionImpl
    val body = overriddenOwner.body
    return when {
        body != null -> body.interpret(newStates)
        else -> calculateOverridden(overriddenOwner, newStates)
    }.apply { data.pushReturnValue(newStates) }
}

private fun isBuiltIn(irFunction: IrFunction): Boolean {
    val descriptor = irFunction.descriptor
    val methodName = descriptor.name.asString()
    val receiverType = descriptor.dispatchReceiverParameter?.type ?: descriptor.extensionReceiverParameter?.type
    val argsType = listOfNotNull(receiverType) + descriptor.valueParameters.map { TypeUtils.makeNotNullable(it.original.type) }
    val signature = CompileTimeFunction(
        methodName,
        argsType.map { it.toString() })
    return (unaryFunctions[signature] ?: binaryFunctions[signature]) != null
}

private fun calculateBuiltIns(expression: IrCall, data: Frame): Code {
    val descriptor = expression.symbol.descriptor
    val methodName = descriptor.name.asString()
    val receiverType = descriptor.dispatchReceiverParameter?.type ?: descriptor.extensionReceiverParameter?.type
    val argsType = listOfNotNull(receiverType) + descriptor.valueParameters.map { TypeUtils.makeNotNullable(it.original.type) }
    val argsValues = data.getAll()
        .map { it.state }
        .map { it as? Primitive<*> ?: throw IllegalArgumentException("Builtin functions accept only const args") }
        .map { it.getIrConst().value }
    val signature = CompileTimeFunction(methodName, argsType.map { it.toString() })
    //todo try catch
    val result = when (argsType.size) {
        1 -> {
            val function = unaryFunctions[signature]
                ?: throw NoSuchMethodException("For given function $signature there is no entry in unary map")
            function.invoke(argsValues.first())
        }
        2 -> {
            val function = binaryFunctions[signature]
                ?: throw NoSuchMethodException("For given function $signature there is no entry in binary map")
            when (methodName) {
                "rangeTo" -> return calculateRangeTo(expression, data)
                else -> function.invoke(argsValues[0], argsValues[1])
            }
        }
        else -> throw UnsupportedOperationException("Unsupported number of arguments")
    }
    data.pushReturnValue(result.toState(expression))
    return Code.NEXT
}

private fun calculateRangeTo(expression: IrExpression, data: Frame): Code {
    val constructor = expression.type.classOrNull!!.owner.constructors.first()
    val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)

    val primitiveValueParameters = data.getAll().map { it.state as Primitive<*> }
    primitiveValueParameters.forEachIndexed { index, primitive -> constructorCall.putValueArgument(index, primitive.getIrConst()) }

    val constructorValueParameters = constructor.valueParameters.map { it.descriptor }.zip(primitiveValueParameters)
    val newFrame = InterpreterFrame(constructorValueParameters.map { Variable(it.first, it.second) }.toMutableList())

    val code = constructorCall.interpret(newFrame)
    data.pushReturnValue(newFrame)
    return code
}

fun IrMemberAccessExpression.interpretValueParameters(data: Frame): Code {
    for (i in (this.valueArgumentsCount - 1) downTo 0) {
        val code = this.getValueArgument(i)?.interpret(data) ?: Code.NEXT
        if (code != Code.NEXT) return code
    }
    return Code.NEXT
}

fun IrCall.interpretCall(data: Frame): Code {
    val newFrame = InterpreterFrame()

    this.interpretValueParameters(data).also { if (it != Code.NEXT) return it }
    val valueParameters = this.symbol.descriptor.valueParameters.map { Variable(it, data.popReturnValue()) }

    val rawReceiver = this.dispatchReceiver ?: this.extensionReceiver
    rawReceiver?.interpret(data)?.also { if (it != Code.NEXT) return it }

    val receiver = rawReceiver?.let { data.popReturnValue() }
    val irFunction = receiver.getIrFunction(this)
    val receiverParameter = irFunction.symbol.getThisAsReceiver()
    // it is important firstly to add receiver, then arguments
    receiver?.let { newFrame.addVar(Variable(receiverParameter!!, it)) }
    newFrame.addAll(valueParameters)

    val code = when {
        //irFunction.annotations.any { it.descriptor.containingDeclaration.fqNameSafe == evaluateIntrinsicAnnotation } -> empty
        isBuiltIn(irFunction) -> calculateBuiltIns(this, newFrame)
        this.isAbstract() -> calculateAbstract(irFunction, newFrame) //abstract check must be before fake overridden check
        this.isFakeOverridden() -> calculateOverridden(irFunction as IrFunctionImpl, newFrame)
        else -> (irFunction.body ?: this.getBody())!!.interpret(newFrame)
    }
    data.pushReturnValue(newFrame)
    return code
}

fun IrFunctionAccessExpression.interpretConstructor(data: Frame): Code {
    this.interpretValueParameters(data).also { if (it != Code.NEXT) return it }
    val valueParameters = this.symbol.descriptor.valueParameters.map { Variable(it, data.popReturnValue()) }.toMutableList()

    val newFrame = InterpreterFrame(valueParameters)
    val state = Complex(this.symbol.owner.parent as IrClass, mutableListOf())
    newFrame.addVar(Variable(this.getThisAsReceiver(), state)) //used to set up fields in body
    val code = this.getBody()?.interpret(newFrame) ?: Code.NEXT
    if (newFrame.hasReturnValue()) {
        state.setSuperQualifier(newFrame.popReturnValue() as Complex)
    }
    data.pushReturnValue(state)
    return code
}

fun IrConstructorCall.interpretConstructorCall(data: Frame): Code {
    return this.interpretConstructor(data)
}

fun IrDelegatingConstructorCall.interpretDelegatedConstructorCall(data: Frame): Code {
    if (this.symbol.descriptor.containingDeclaration.defaultType == DefaultBuiltIns.Instance.anyType) {
        return Code.NEXT
    }

    return this.interpretConstructor(data)
}

fun IrConst<*>.interpretConst(data: Frame): Code {
    data.pushReturnValue(this.toPrimitive())
    return Code.NEXT
}

fun List<IrStatement>.interpretStatements(data: Frame): Code {
    //create newFrame
    val newFrame = data.copy()

    var code = Code.NEXT
    val iterator = this.asSequence().iterator()
    while (code == Code.NEXT && iterator.hasNext()) {
        code = iterator.next().interpret(newFrame)
    }
    data.pushReturnValue(newFrame)
    return code
}

fun IrBlock.interpretBlock(data: Frame): Code {
    return this.statements.interpretStatements(data)
}

fun IrBody.interpretBody(data: Frame): Code {
    return this.statements.interpretStatements(data)
}

fun IrReturn.interpretReturn(data: Frame): Code {
    val code = this.value.interpret(data)
    return if (code == Code.NEXT) Code.RETURN else code
}

fun IrWhileLoop.interpretWhile(data: Frame): Code {
    var code = Code.NEXT
    while (code == Code.NEXT) {
        code = this.condition.interpret(data)
        if (code == Code.NEXT && (data.popReturnValue() as? Primitive<*>)?.getIrConst()?.value as? Boolean == true) {
            code = this.body?.interpret(data) ?: Code.NEXT
        } else {
            break
        }
    }
    return code
}

fun IrWhen.interpretWhen(data: Frame): Code {
    var code = Code.NEXT
    val iterator = this.branches.asSequence().iterator()
    while (code == Code.NEXT && iterator.hasNext()) {
        code = iterator.next().interpret(data)
    }
    return code
}

fun IrBranch.interpretBranch(data: Frame): Code {
    var code = this.condition.interpret(data)
    if (code == Code.NEXT && (data.popReturnValue() as? Primitive<*>)?.getIrConst()?.value as? Boolean == true) {
        code = this.result.interpret(data)
        if (code == Code.NEXT) return Code.BREAK_WHEN
    }
    return code
}

fun IrBreak.interpretBreak(data: Frame): Code {
    return Code.BREAK_LOOP.apply { info = this@interpretBreak.label ?: "" }
}

fun IrSetField.interpretSetField(data: Frame): Code {
    val code = this.value.interpret(data)
    if (code != Code.NEXT) return code

    val receiver = (this.receiver as IrDeclarationReference).symbol.descriptor
    data.getVariableState(receiver).setState(Variable(this.symbol.owner.descriptor, data.popReturnValue()))
    return Code.NEXT
}

fun IrGetField.interpretGetField(data: Frame): Code {
    val receiver = (this.receiver as? IrDeclarationReference)?.symbol?.descriptor // receiver is null, for example, for top level fields
    val result = receiver?.let { data.getVariableState(receiver).getState(this.symbol.descriptor)?.copy() }
    if (result == null) {
        return this.symbol.owner.initializer?.expression?.interpret(data) ?: Code.NEXT
    }
    data.pushReturnValue(result)
    return Code.NEXT
}

fun IrGetValue.interpretGetValue(data: Frame): Code {
    data.pushReturnValue(data.getVariableState(this.symbol.descriptor).copy())
    return Code.NEXT
}

fun IrVariable.interpretVariable(data: Frame): Code {
    val code = this.initializer?.interpret(data)
    if (code != Code.NEXT) return code ?: Code.NEXT
    data.addVar(Variable(this.descriptor, data.popReturnValue()))
    return Code.NEXT
}

fun IrSetVariable.interpretSetVariable(data: Frame): Code {
    val code = this.value.interpret(data)
    if (code != Code.NEXT) return code

    if (data.contains(this.symbol.descriptor)) {
        val variable = data.getVariableState(this.symbol.descriptor)
        variable.setState(Variable(this.symbol.descriptor, data.popReturnValue()))
    } else {
        data.addVar(Variable(this.symbol.descriptor, data.popReturnValue()))
    }
    return Code.NEXT
}

fun IrGetObjectValue.interpretGetObjectValue(data: Frame): Code {
    data.pushReturnValue(Complex(this.symbol.owner, mutableListOf()))
    return Code.NEXT
}

fun IrTypeOperatorCall.interpretTypeOperatorCall(data: Frame): Code {
    return when (this.operator) {
        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
            this.argument.interpret(data)
        }
        IrTypeOperator.CAST -> {
            this.argument.interpret(data) //todo check cast correctness
        }
        else -> TODO("${this.operator} not implemented")
    }
}