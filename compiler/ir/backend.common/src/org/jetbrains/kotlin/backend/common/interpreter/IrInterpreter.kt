/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.builtins.*
import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.TypeUtils
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

enum class Code(var info: String = "") {
    NEXT, RETURN, BREAK_LOOP, BREAK_WHEN, CONTINUE, EXCEPTION
}

class IrInterpreter(private val irBuiltIns: IrBuiltIns) {

    fun interpret(expression: IrExpression): IrExpression {
        return InterpreterFrame().apply { expression.interpret(this) }.popReturnValue().toIrExpression(irBuiltIns, expression)
    }

    private fun IrElement.interpret(data: Frame): Code {
        try {
            val code = when (this) {
                is IrFunctionImpl -> interpretFunction(this, data)
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
                is IrContinue -> interpretContinue(this, data)

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
                Code.CONTINUE -> when (this) {
                    is IrWhileLoop -> if ((this.label ?: "") == code.info) this.interpret(data) else code
                    else -> code
                }
                Code.EXCEPTION -> TODO("Code.EXCEPTION not implemented")
                Code.NEXT -> Code.NEXT
            }
        } catch (e: Exception) {
            e.printStackTrace()
            assert(false)
            return Code.EXCEPTION
        }
    }

    // this method is used to get stack trace after exception
    private fun interpretFunction(irFunction: IrFunctionImpl, data: Frame): Code {
        return irFunction.body?.interpret(data) ?: throw AssertionError("Ir function must be with body")
    }

    private fun calculateAbstract(irFunction: IrFunction, data: Frame): Code {
        if (irFunction.body == null) {
            val receiver = data.getVariableState(irFunction.symbol.getReceiver()!!) as Complex
            val instance = receiver.instance!!

            val functionImplementation = instance.getIrFunction(irFunction.descriptor)
            if (functionImplementation?.body == null) throw NoSuchMethodException("Method \"${irFunction.name}\" wasn't implemented")
            val arguments = functionImplementation.valueParameters.map { Variable(it.descriptor, data.getVariableState(it.descriptor)) }
            val newFrame = InterpreterFrame()
            newFrame.addVar(Variable(functionImplementation.symbol.getReceiver()!!, instance))
            newFrame.addAll(arguments)
            return functionImplementation.interpret(newFrame).apply { data.pushReturnValue(newFrame) }
        }
        return irFunction.body!!.interpret(data)
    }

    private fun calculateOverridden(owner: IrFunctionImpl, data: Frame): Code {
        val variableDescriptor = owner.symbol.getReceiver()!!
        val superQualifier = (data.getVariableState(variableDescriptor) as Complex).superType!!
        val overridden = owner.overriddenSymbols.first { it.getReceiver()?.equalTo(superQualifier.getReceiver()) == true }

        val valueParameters = owner.valueParameters.zip(overridden.owner.valueParameters)
            .map { Variable(it.second.descriptor, data.getVariableState(it.first.descriptor)) }
        val newStates = InterpreterFrame(valueParameters.toMutableList())
        newStates.addVar(Variable(overridden.getReceiver()!!, superQualifier))

        val overriddenOwner = overridden.owner as IrFunctionImpl
        return when {
            overriddenOwner.body != null -> overriddenOwner.interpret(newStates)
            else -> calculateOverridden(overriddenOwner, newStates)
        }.apply { data.pushReturnValue(newStates) }
    }

    private fun calculateBuiltIns(irFunction: IrFunction, data: Frame): Code {
        val descriptor = irFunction.descriptor
        val methodName = descriptor.name.asString()
        val receiverType = descriptor.dispatchReceiverParameter?.type ?: descriptor.extensionReceiverParameter?.type
        val argsType = listOfNotNull(receiverType) + descriptor.valueParameters.map { TypeUtils.makeNotNullable(it.original.type) }
        val argsValues = data.getAll()
            .map { it.state }
            .map { it as? Primitive<*> ?: throw IllegalArgumentException("Builtin functions accept only const args") }
            .map { it.getValue() }
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
                    "rangeTo" -> return calculateRangeTo(irFunction.returnType, data)
                    else -> function.invoke(argsValues[0], argsValues[1])
                }
            }
            3 -> {
                val function = ternaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in ternary map")
                function.invoke(argsValues[0], argsValues[1], argsValues[2])
            }
            else -> throw UnsupportedOperationException("Unsupported number of arguments")
        }
        data.pushReturnValue(result.toState(irBuiltIns))
        return Code.NEXT
    }

    private fun calculateRangeTo(type: IrType, data: Frame): Code {
        val constructor = type.classOrNull!!.owner.constructors.first()
        val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)

        val primitiveValueParameters = data.getAll().map { it.state as Primitive<*> }
        primitiveValueParameters.forEachIndexed { index, primitive -> constructorCall.putValueArgument(index, primitive.getValue().toIrConst(irBuiltIns)) }

        val constructorValueParameters = constructor.valueParameters.map { it.descriptor }.zip(primitiveValueParameters)
        val newFrame = InterpreterFrame(constructorValueParameters.map { Variable(it.first, it.second) }.toMutableList())

        val code = constructorCall.interpret(newFrame)
        data.pushReturnValue(newFrame)
        return code
    }

    private fun calculateIntrinsic(irFunction: IrFunction, data: Frame): Code {
        val annotation = irFunction.getAnnotation(evaluateIntrinsicAnnotation)
        val argsValues = data.getAll()
            .map { it.state }
            .map { it as? Primitive<*> ?: throw IllegalArgumentException("Builtin functions accept only const args") }
            .map { it.getValue() }

        val textClass = Class.forName((annotation.getValueArgument(0) as IrConst<*>).value.toString())
        val returnClass = irFunction.returnType.getFqName()!!.let { getPrimitiveClass(it) ?: Class.forName(it) }
        val extensionClass = irFunction.extensionReceiverParameter?.type?.getFqName()?.let { getPrimitiveClass(it) ?: Class.forName(it) }
        val argsClasses = irFunction.valueParameters.map {
            it.descriptor.fqNameSafe.asString().let { getPrimitiveClass(it) ?: Class.forName(it) }
        }

        val methodSignature = MethodType.methodType(returnClass, listOfNotNull(extensionClass) + argsClasses)
        val method = MethodHandles.lookup().findStatic(textClass, irFunction.name.asString(), methodSignature)
        val result = method.invokeWithArguments(argsValues)
        data.pushReturnValue(result.toState(irBuiltIns))
        return Code.NEXT
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

        // dispatch receiver processing
        val rawDispatchReceiver = expression.dispatchReceiver
        rawDispatchReceiver?.interpret(data)?.also { if (it != Code.NEXT) return it }
        val irCallReceiver = rawDispatchReceiver?.let { data.popReturnValue() }
        val irFunctionReceiver = if (expression.superQualifierSymbol == null) irCallReceiver else (irCallReceiver as Complex).superType
        // it is important firstly to add receiver, then arguments; this order is used in builtin method call
        val irFunction = irFunctionReceiver?.getIrFunction(expression.symbol.descriptor) ?: expression.symbol.owner
        irFunctionReceiver?.let { newFrame.addVar(Variable(irFunction.symbol.getDispatchReceiver()!!, it)) }

        // extension receiver processing
        val rawExtensionReceiver = expression.extensionReceiver
        rawExtensionReceiver?.interpret(data)?.also { if (it != Code.NEXT) return it }
        rawExtensionReceiver?.let { newFrame.addVar(Variable(irFunction.symbol.getExtensionReceiver()!!, data.popReturnValue())) }

        newFrame.addAll(valueParameters)

        val code = when {
            irFunction.hasAnnotation(evaluateIntrinsicAnnotation) -> calculateIntrinsic(irFunction, newFrame)
            irFunction.isAbstract() -> calculateAbstract(irFunction, newFrame) //abstract check must be before fake overridden check
            irFunction.isFakeOverridden() -> calculateOverridden(irFunction as IrFunctionImpl, newFrame)
            irFunction.body == null -> calculateBuiltIns(irFunction, newFrame)
            else -> irFunction.interpret(newFrame)
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
            state.superType = newFrame.popReturnValue() as Complex
        }
        data.pushReturnValue(state)
        return code
    }

    private fun interpretConstructorCall(constructorCall: IrConstructorCall, data: Frame): Code {
        return interpretConstructor(constructorCall, data).apply {
            val instance = data.peekReturnValue() as Complex
            instance.setInstanceRecursive(instance)
        }
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
            if (code == Code.NEXT && (data.popReturnValue() as? Primitive<*>)?.getValue() as? Boolean == true) {
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
        if (code == Code.NEXT && (data.popReturnValue() as? Primitive<*>)?.getValue() as? Boolean == true) {
            code = expression.result.interpret(data)
            if (code == Code.NEXT) return Code.BREAK_WHEN
        }
        return code
    }

    private fun interpretBreak(breakStatement: IrBreak, data: Frame): Code {
        return Code.BREAK_LOOP.apply { info = breakStatement.label ?: "" }
    }

    private fun interpretContinue(continueStatement: IrContinue, data: Frame): Code {
        return Code.CONTINUE.apply { info = continueStatement.label ?: "" }
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