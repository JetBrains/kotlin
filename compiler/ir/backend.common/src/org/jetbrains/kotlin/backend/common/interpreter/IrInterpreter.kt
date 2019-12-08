/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.interpreter.builtins.CompileTimeFunction
import org.jetbrains.kotlin.backend.common.interpreter.builtins.binaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.builtins.unaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.backend.common.ir.Symbols
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

class IrInterpreter(private val irSymbols: Symbols<CommonBackendContext>) {

    fun interpret(expression: IrExpression): IrExpression {
        return InterpreterFrame().apply { expression.interpret(this) }.popReturnValue().toIrExpression(expression)
    }

    private fun IrElement.interpret(data: Frame): Code {
        try {
            val code = when (this) {
                is IrCall -> interpretCall(this, data)
                is IrConstructorCall -> interpretConstructorCall(this, data)
                is IrDelegatingConstructorCall -> interpretDelegatedConstructorCall(this, data)
                is IrBody -> interpretBody(this, data)
                is IrBlock -> interpretBlock(this, data)
                is IrReturn -> interpretReturn(this, data)
                is IrSetField -> interpretSetField(this, data)
                is IrGetField -> interpretGetField(this, data)
                is IrGetValue -> interpretGetValue(this, data)
                is IrGetObjectValue -> interpretGetObjectValue(this, data)
                is IrConst<*> -> interpretConst(this, data)
                is IrVariable -> interpretVariable(this, data)
                is IrSetVariable -> interpretSetVariable(this, data)
                is IrTypeOperatorCall -> interpretTypeOperatorCall(this, data)
                is IrBranch -> interpretBranch(this, data)
                is IrWhileLoop -> interpretWhile(this, data)
                is IrWhen -> interpretWhen(this, data)
                is IrBreak -> interpretBreak(this, data)

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

    private fun interpretValueParameters(parametersContainer: IrMemberAccessExpression, data: Frame): Code {
        for (i in (parametersContainer.valueArgumentsCount - 1) downTo 0) {
            val code = parametersContainer.getValueArgument(i)?.interpret(data) ?: Code.NEXT
            if (code != Code.NEXT) return code
        }
        return Code.NEXT
    }

    private fun interpretCall(expression: IrCall, data: Frame): Code {
        val newFrame = InterpreterFrame()

        interpretValueParameters(expression, data).also { if (it != Code.NEXT) return it }
        val valueParameters = expression.symbol.descriptor.valueParameters.map { Variable(it, data.popReturnValue()) }

        val rawReceiver = expression.dispatchReceiver ?: expression.extensionReceiver
        rawReceiver?.interpret(data)?.also { if (it != Code.NEXT) return it }

        val receiver = rawReceiver?.let { data.popReturnValue() }
        val irFunction = receiver.getIrFunction(expression)
        val receiverParameter = irFunction.symbol.getThisAsReceiver()
        // it is important firstly to add receiver, then arguments
        receiver?.let { newFrame.addVar(Variable(receiverParameter!!, it)) }
        newFrame.addAll(valueParameters)

        val code = when {
            //irFunction.annotations.any { it.descriptor.containingDeclaration.fqNameSafe == evaluateIntrinsicAnnotation } -> empty
            isBuiltIn(irFunction) -> calculateBuiltIns(expression, newFrame)
            expression.isAbstract() -> calculateAbstract(
                irFunction,
                newFrame
            ) //abstract check must be before fake overridden check
            expression.isFakeOverridden() -> calculateOverridden(irFunction as IrFunctionImpl, newFrame)
            else -> (irFunction.body ?: expression.getBody())!!.interpret(newFrame)
        }
        data.pushReturnValue(newFrame)
        return code
    }

    private fun interpretConstructor(constructorCall: IrFunctionAccessExpression, data: Frame): Code {
        interpretValueParameters(constructorCall, data).also { if (it != Code.NEXT) return it }
        val valueParameters = constructorCall.symbol.descriptor.valueParameters.map { Variable(it, data.popReturnValue()) }.toMutableList()

        val newFrame = InterpreterFrame(valueParameters)
        val state = Complex(constructorCall.symbol.owner.parent as IrClass, mutableListOf())
        newFrame.addVar(Variable(constructorCall.getThisAsReceiver(), state)) //used to set up fields in body
        val code = constructorCall.getBody()?.interpret(newFrame) ?: Code.NEXT
        if (newFrame.hasReturnValue()) {
            state.setSuperQualifier(newFrame.popReturnValue() as Complex)
        }
        data.pushReturnValue(state)
        return code
    }

    private fun interpretConstructorCall(constructorCall: IrConstructorCall, data: Frame): Code {
        return interpretConstructor(constructorCall, data)
    }

    private fun interpretDelegatedConstructorCall(delegatingConstructorCall: IrDelegatingConstructorCall, data: Frame): Code {
        if (delegatingConstructorCall.symbol.descriptor.containingDeclaration.defaultType == DefaultBuiltIns.Instance.anyType) {
            return Code.NEXT
        }

        return interpretConstructor(delegatingConstructorCall, data)
    }

    private fun interpretConst(expression: IrConst<*>, data: Frame): Code {
        data.pushReturnValue(expression.toPrimitive())
        return Code.NEXT
    }

    private fun interpretStatements(statements: List<IrStatement>, data: Frame): Code {
        //create newFrame
        val newFrame = data.copy()

        var code = Code.NEXT
        val iterator = statements.asSequence().iterator()
        while (code == Code.NEXT && iterator.hasNext()) {
            code = iterator.next().interpret(newFrame)
        }
        data.pushReturnValue(newFrame)
        return code
    }

    private fun interpretBlock(block: IrBlock, data: Frame): Code {
        return interpretStatements(block.statements, data)
    }

    private fun interpretBody(body: IrBody, data: Frame): Code {
        return interpretStatements(body.statements, data)
    }

    private fun interpretReturn(expression: IrReturn, data: Frame): Code {
        val code = expression.value.interpret(data)
        return if (code == Code.NEXT) Code.RETURN else code
    }

    private fun interpretWhile(expression: IrWhileLoop, data: Frame): Code {
        var code = Code.NEXT
        while (code == Code.NEXT) {
            code = expression.condition.interpret(data)
            if (code == Code.NEXT && (data.popReturnValue() as? Primitive<*>)?.getIrConst()?.value as? Boolean == true) {
                code = expression.body?.interpret(data) ?: Code.NEXT
            } else {
                break
            }
        }
        return code
    }

    private fun interpretWhen(expression: IrWhen, data: Frame): Code {
        var code = Code.NEXT
        val iterator = expression.branches.asSequence().iterator()
        while (code == Code.NEXT && iterator.hasNext()) {
            code = iterator.next().interpret(data)
        }
        return code
    }

    private fun interpretBranch(expression: IrBranch, data: Frame): Code {
        var code = expression.condition.interpret(data)
        if (code == Code.NEXT && (data.popReturnValue() as? Primitive<*>)?.getIrConst()?.value as? Boolean == true) {
            code = expression.result.interpret(data)
            if (code == Code.NEXT) return Code.BREAK_WHEN
        }
        return code
    }

    private fun interpretBreak(breakStatement: IrBreak, data: Frame): Code {
        return Code.BREAK_LOOP.apply { info = breakStatement.label ?: "" }
    }

    private fun interpretSetField(expression: IrSetField, data: Frame): Code {
        val code = expression.value.interpret(data)
        if (code != Code.NEXT) return code

        val receiver = (expression.receiver as IrDeclarationReference).symbol.descriptor
        data.getVariableState(receiver).setState(Variable(expression.symbol.owner.descriptor, data.popReturnValue()))
        return Code.NEXT
    }

    private fun interpretGetField(expression: IrGetField, data: Frame): Code {
        val receiver = (expression.receiver as? IrDeclarationReference)?.symbol?.descriptor // receiver is null, for example, for top level fields
        val result = receiver?.let { data.getVariableState(receiver).getState(expression.symbol.descriptor)?.copy() }
        if (result == null) {
            return expression.symbol.owner.initializer?.expression?.interpret(data) ?: Code.NEXT
        }
        data.pushReturnValue(result)
        return Code.NEXT
    }

    private fun interpretGetValue(expression: IrGetValue, data: Frame): Code {
        data.pushReturnValue(data.getVariableState(expression.symbol.descriptor).copy())
        return Code.NEXT
    }

    private fun interpretVariable(expression: IrVariable, data: Frame): Code {
        val code = expression.initializer?.interpret(data)
        if (code != Code.NEXT) return code ?: Code.NEXT
        data.addVar(Variable(expression.descriptor, data.popReturnValue()))
        return Code.NEXT
    }

    private fun interpretSetVariable(expression: IrSetVariable, data: Frame): Code {
        val code = expression.value.interpret(data)
        if (code != Code.NEXT) return code

        if (data.contains(expression.symbol.descriptor)) {
            val variable = data.getVariableState(expression.symbol.descriptor)
            variable.setState(Variable(expression.symbol.descriptor, data.popReturnValue()))
        } else {
            data.addVar(Variable(expression.symbol.descriptor, data.popReturnValue()))
        }
        return Code.NEXT
    }

    private fun interpretGetObjectValue(expression: IrGetObjectValue, data: Frame): Code {
        data.pushReturnValue(Complex(expression.symbol.owner, mutableListOf()))
        return Code.NEXT
    }

    private fun interpretTypeOperatorCall(expression: IrTypeOperatorCall, data: Frame): Code {
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                expression.argument.interpret(data)
            }
            IrTypeOperator.CAST -> {
                expression.argument.interpret(data) //todo check cast correctness
            }
            else -> TODO("${expression.operator} not implemented")
        }
    }

}