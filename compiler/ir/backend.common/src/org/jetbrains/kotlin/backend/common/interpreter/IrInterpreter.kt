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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.TypeUtils

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
    //todo move out util methods
    private fun IrElement?.getState(descriptor: DeclarationDescriptor, data: Frame): State? {
        val arg = this?.accept(this@IrInterpreter, data) ?: return null
        return arg.setDescriptor(descriptor)
    }

    private fun IrMemberAccessExpression.convertValueParameters(data: Frame): MutableList<State> {
        val state = mutableListOf<State>()
        for (i in 0 until this.valueArgumentsCount) {
            this.getValueArgument(i).getState((this.symbol.descriptor as FunctionDescriptor).valueParameters[i], data)?.let { state += it }
        }
        return state
    }

    private fun calculateOverridden(symbol: IrFunctionSymbol, data: Frame): State {
        val owner = symbol.owner as IrFunctionImpl
        val overridden = owner.overriddenSymbols.first()
        val overriddenReceiver = data.getVar(symbol.getThisAsReceiver()).getState(overridden.getThisAsReceiver())
        val valueParameters = symbol.owner.valueParameters.zip(overridden.owner.valueParameters)
            .map { data.getVar(it.first.descriptor).setDescriptor(it.second.descriptor) }
        val newStates = InterpreterFrame((valueParameters + overriddenReceiver).toMutableList())

        return if (overridden.owner.body != null) {
            overridden.owner.body!!.accept(this@IrInterpreter, newStates)
        } else {
            calculateOverridden(overridden.owner.symbol, newStates)
        }
    }

    private fun calculateBuiltIns(descriptor: FunctionDescriptor, frame: Frame): Any {
        val methodName = descriptor.name.asString()
        val receiverType = descriptor.dispatchReceiverParameter?.type ?: descriptor.extensionReceiverParameter?.type
        val argsType = listOfNotNull(receiverType) + descriptor.valueParameters.map { TypeUtils.makeNotNullable(it.original.type) }
        val argsValues = frame.getAll()
            .map { it as? Primitive<*> ?: throw IllegalArgumentException("Builtin functions accept only const args") }
            .map { it.getIrConst().value }
        val signature = CompileTimeFunction(methodName, argsType.map { it.toString() })
        return when (argsType.size) {
            1 -> {
                val function = unaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in unary map")
                function.invoke(argsValues.first())
            }
            2 -> {
                val function = binaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in binary map")
                function.invoke(argsValues[1], argsValues[0])
            }
            else -> throw UnsupportedOperationException("Unsupported number of arguments")
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
            else -> TODO("${element.javaClass} not supported")
        }
    }

    override fun visitCall(expression: IrCall, data: Frame): State {
        val newFrame = InterpreterFrame(expression.convertValueParameters(data))
        if (expression.symbol.owner.isFakeOverride) {
            expression.dispatchReceiver?.accept(this, data)?.let { newFrame.addVar(it) }
            return calculateOverridden(expression.symbol, newFrame)
        }

        val dispatchReceiver = expression.dispatchReceiver?.accept(this, data)
            ?.setDescriptor(expression.symbol.descriptor.dispatchReceiverParameter!!)
        val extensionReceiver = expression.extensionReceiver?.accept(this, data)
            ?.setDescriptor(expression.symbol.descriptor.extensionReceiverParameter?.containingDeclaration!!)
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
        val newFrame = InterpreterFrame(constructor.convertValueParameters(data))
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
        val receiver = (expression.receiver as IrDeclarationReference).symbol.descriptor
        return data.getVar(receiver).getState(expression.symbol.descriptor).copy()
    }

    override fun visitGetValue(expression: IrGetValue, data: Frame): State {
        return data.getVar(expression.symbol.descriptor).copy()
    }
}
