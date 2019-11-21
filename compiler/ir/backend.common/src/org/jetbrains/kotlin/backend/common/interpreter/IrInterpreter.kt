/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrInterpreter : IrElementVisitor<State, Frame> {
    private val builtIns = DefaultBuiltIns.Instance
    private val empty = EmptyState()

    fun interpret(expression: IrExpression): IrExpression {
        return visitExpression(expression, InterpreterFrame()).toIrExpression(expression)
    }

    override fun visitElement(element: IrElement, data: Frame): State {
        return when (element) {
            is IrCall -> visitCall(element, data)
            is IrConstructorCall -> visitConstructor(element, data)
            is IrDelegatingConstructorCall -> visitDelegatingConstructorCall(element, data)
            is IrBody -> visitStatements(element.statements, data)
            is IrBlock -> visitStatements(element.statements, data)
            is IrSetField -> visitSetField(element, data)
            is IrGetField -> visitGetField(element, data)
            is IrGetValue -> visitGetValue(element, data)
            is IrGetObjectValue -> visitGetObjectValue(element, data)
            is IrConst<*> -> visitConst(element, data)
            is IrWhen -> visitWhen(element, data)
            else -> TODO("${element.javaClass} not supported")
        }
    }

    override fun visitCall(expression: IrCall, data: Frame): State {
        val newFrame = InterpreterFrame()
        val valueParameters = convertValueParameters(expression, data)

        val dispatchReceiver = expression.dispatchReceiver?.accept(this, data)      // can be either Primitive or Complex
        val extensionReceiver = expression.extensionReceiver?.accept(this, data)    // similarly

        val irFunction = dispatchReceiver.getIrFunction(expression)
        // it is important firstly to add receiver, then arguments
        val receiverParameter = irFunction.symbol.getReceiverDescriptor()
        val receiver = (dispatchReceiver ?: extensionReceiver)
        receiver?.let { newFrame.addVar(Variable(receiverParameter!!, it)) }
        newFrame.addAll(valueParameters)

        return when {
            expression.isAbstract() -> calculateAbstract(irFunction, newFrame) //abstract check must be first
            expression.isFakeOverridden() -> calculateOverridden(irFunction as IrFunctionImpl, newFrame)
            expression.getBody() == null -> calculateBuiltIns(expression, newFrame).toState(expression)
            else -> (irFunction.body ?: expression.getBody())!!.accept(this, newFrame)
        }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Frame): State {
        return expression.toPrimitive()
    }

    private fun visitConstructor(constructor: IrFunctionAccessExpression, data: Frame): State {
        val newFrame = InterpreterFrame(convertValueParameters(constructor, data))
        val obj = Complex(constructor.symbol.owner.parent as IrClass, mutableListOf())
        constructor.getBody()?.statements?.forEach {
            when (it) {
                is IrDelegatingConstructorCall -> {
                    val delegatingConstructorCall = visitDelegatingConstructorCall(it, newFrame)
                    if (delegatingConstructorCall != empty) {
                        val superObj = Variable(it.getThisAsReceiver(), delegatingConstructorCall)
                        obj.addSuperQualifier(superObj)
                    }
                    newFrame.addVar(Variable(constructor.getThisAsReceiver(), obj))
                }
                else -> it.accept(this, newFrame)
            }
        }
        return obj
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Frame): State {
        if (expression.symbol.descriptor.containingDeclaration.defaultType == builtIns.anyType) {
            return empty
        }

        return visitConstructor(expression, data)
    }

    private fun visitStatements(statements: List<IrStatement>, data: Frame): State {
        statements.forEachIndexed { index, statement ->
            when {
                statement is IrReturn || index == statements.lastIndex -> return statement.accept(this, data)
                else -> statement.accept(this, data)
            }
        }

        // unreachable state; method must return inside forEach
        return empty
    }

    override fun visitReturn(expression: IrReturn, data: Frame): State {
        return expression.value.accept(this, data)
    }

    override fun visitSetField(expression: IrSetField, data: Frame): State {
        val value = expression.value.accept(this, data)
        val receiver = (expression.receiver as IrDeclarationReference).symbol.descriptor
        data.getVariableState(receiver).setState(Variable(expression.symbol.owner.descriptor, value))
        return empty
    }

    override fun visitGetField(expression: IrGetField, data: Frame): State {
        val receiver = (expression.receiver as? IrDeclarationReference)?.symbol?.descriptor // receiver is null, for example, for top level fields
        return receiver?.let { data.getVariableState(receiver).getState(expression.symbol.descriptor)?.copy() }
            ?: expression.symbol.owner.initializer!!.expression.accept(this, data)
    }

    override fun visitGetValue(expression: IrGetValue, data: Frame): State {
        return data.getVariableState(expression.symbol.descriptor).copy()
    }

    override fun visitVariable(declaration: IrVariable, data: Frame): State {
        val variable = declaration.initializer?.accept(this, data)
        variable?.let { data.addVar(Variable(declaration.descriptor, it)) }
        return empty
    }

    override fun visitSetVariable(expression: IrSetVariable, data: Frame): State {
        if (data.contains(expression.symbol.descriptor)) {
            val variable = data.getVariableState(expression.symbol.descriptor)
            variable.setState(Variable(expression.symbol.descriptor, expression.value.accept(this, data)))
        } else {
            val variable = expression.value.accept(this, data)
            data.addVar(Variable(expression.symbol.descriptor, variable))
        }
        return empty
    }

    override fun visitWhen(expression: IrWhen, data: Frame): State {
        expression.branches.forEach {
            if ((it.condition.accept(this, data) as? Primitive<*>)?.getIrConst()?.value == true) {
                return it.result.accept(this, data)
            }
        }
        return empty
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue, data: Frame): State {
        return Complex(expression.symbol.owner, mutableListOf())
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Frame): State {
        when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                expression.argument.accept(this, data)
                return empty
            }
            else -> TODO("${expression.operator} not implemented")
        }
    }

    override fun visitWhileLoop(loop: IrWhileLoop, data: Frame): State {
        while ((loop.condition.accept(this, data) as? Primitive<*>)?.getIrConst()?.value == true) {
            loop.body?.accept(this, InterpreterFrame(data.getAll().toMutableList()))
        }
        return empty
    }
}
