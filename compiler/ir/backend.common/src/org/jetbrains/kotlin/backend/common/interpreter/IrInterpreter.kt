/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrInterpreter : IrElementVisitor<State, Frame> {
    private val builtIns = DefaultBuiltIns.Instance
    private val unit = Complex(builtIns.unit, mutableListOf())
    private val any = Complex(builtIns.any, mutableListOf())

    fun interpret(expression: IrExpression): IrExpression {
        return visitExpression(expression, InterpreterFrame()).toIrExpression()
    }

    private fun State.toIrExpression(): IrExpression {
        return when (this) {
            is Primitive<*> -> this.getIrConst()
            else -> TODO("not supported")
        }
    }

    override fun visitElement(element: IrElement, data: Frame): State {
        return when (element) {
            is IrCall -> visitCall(element, data)
            is IrConstructor -> visitConstructor(element, data)
            is IrDelegatingConstructorCall -> visitDelegatingConstructorCall(element, data)
            is IrBody -> visitStatements(element.statements, data)
            is IrBlock -> visitStatements(element.statements, data)
            is IrSetField -> visitSetField(element, data)
            is IrGetField -> visitGetField(element, data)
            is IrGetValue -> visitGetValue(element, data)
            is IrConst<*> -> visitConst(element, data)
            else -> TODO("${element.javaClass} not supported")
        }
    }

    override fun visitCall(expression: IrCall, data: Frame): State {
        val newFrame = InterpreterFrame(convertValueParameters(expression, data))
        if (expression.symbol.owner.isFakeOverride) {
            expression.dispatchReceiver?.accept(this, data)?.let { newFrame.addVar(it) }
            return calculateOverridden(expression.symbol, newFrame)
        }

        val dispatchReceiver = expression.dispatchReceiver?.accept(this, data)
            ?.setDescriptor(expression.symbol.descriptor.dispatchReceiverParameter!!)
        val extensionReceiver = expression.extensionReceiver?.accept(this, data)
            ?.setDescriptor(expression.symbol.descriptor.extensionReceiverParameter!!)
        (dispatchReceiver ?: extensionReceiver)?.also { newFrame.addVar(it) }

        return if (expression.getBody() == null) {
            calculateBuiltIns(expression.symbol.descriptor, newFrame).toIrConst(expression).toPrimitive()
        } else {
            expression.getBody()!!.accept(this, newFrame)
        }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Frame): State {
        return expression.toPrimitive()
    }

    private fun visitConstructor(constructor: IrFunctionAccessExpression, data: Frame): State {
        val newFrame = InterpreterFrame(convertValueParameters(constructor, data))
        val obj = Complex((constructor.symbol.descriptor.containingDeclaration as ClassDescriptor).thisAsReceiverParameter, mutableListOf())
        constructor.getBody()?.statements?.forEach {
            when (it) {
                is IrDelegatingConstructorCall -> {
                    obj.addSuperQualifier(visitDelegatingConstructorCall(it, newFrame) as Complex)
                    newFrame.addVar(obj)
                }
                else -> it.accept(this, newFrame)
            }
        }
        return obj
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: Frame): State {
        return visitConstructor(expression, data)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Frame): State {
        if (expression.symbol.descriptor.containingDeclaration.defaultType == builtIns.anyType) {
            return any
        }

        return visitConstructor(expression, data)
    }

    private fun visitStatements(statements: List<IrStatement>, data: Frame): State {
        statements.forEach {
            when (it) {
                is IrReturn -> return it.accept(this, data)
                else -> it.accept(this, data)
            }
        }

        return unit
    }

    override fun visitReturn(expression: IrReturn, data: Frame): State {
        return expression.value.accept(this, data)
    }

    override fun visitSetField(expression: IrSetField, data: Frame): State {
        val value = expression.value.accept(this, data).setDescriptor(expression.symbol.owner.descriptor)
        val receiver = (expression.receiver as IrDeclarationReference).symbol.descriptor
        data.getVar(receiver).setState(value)
        return unit
    }

    override fun visitGetField(expression: IrGetField, data: Frame): State {
        val receiver = (expression.receiver as? IrDeclarationReference)?.symbol?.descriptor // receiver is null, for example, for top level fields
            ?: return expression.symbol.owner.initializer!!.expression.accept(this, data)
        return data.getVar(receiver).getState(expression.symbol.descriptor).copy()
    }

    override fun visitGetValue(expression: IrGetValue, data: Frame): State {
        return data.getVar(expression.symbol.descriptor).copy()
    }
}
