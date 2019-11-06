/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.builtins.CompileTimeFunction
import org.jetbrains.kotlin.backend.common.interpreter.builtins.binaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.builtins.unaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.TypeUtils

class IrInterpreter : IrElementVisitor<State, Frame> {
    private val unit = Complex(null, emptyList())

    fun interpret(expression: IrExpression): IrExpression {
        return visitExpression(expression, InterpreterFrame()).toIrExpression()
    }

    private fun State.toIrExpression(): IrExpression {
        return when (this) {
            is Primitive<*> -> this.getIrConst()
            else -> TODO("not supported")
        }
    }

    private fun IrElement?.getState(descriptor: DeclarationDescriptor, data: Frame): State? {
        val arg = this?.accept(this@IrInterpreter, data) ?: return null
        return arg.setDescriptor(descriptor)
    }

    private fun IrMemberAccessExpression.convertValueParameters(data: Frame): MutableList<State> {
        val state = mutableListOf<State>()
        for (i in 0 until this.valueArgumentsCount) {
            this.getValueArgument(i).getState(this.symbol.descriptor.valueParameters[i], data)?.let { state += it }
        }
        return state
    }

    private fun IrFunctionSymbol.caclOverridden(states: List<State>): State {
        val owner = this.owner as IrFunctionImpl
        val overridden = owner.overriddenSymbols.first()
        val newStates = states.first { it.getDescriptor() == this.descriptor.containingDeclaration }.getState().toMutableList()

        return if (overridden.owner.body != null) {
            val temp = newStates.first { it.getDescriptor() == overridden.descriptor.containingDeclaration }
            overridden.owner.body!!.accept(this@IrInterpreter, InterpreterFrame(mutableListOf(temp)))
        } else {
            overridden.caclOverridden(newStates)
        }
    }

    private fun IrFunctionAccessExpression.getBody(): IrBody? {
        return this.symbol.owner.body
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
                function.invoke(argsValues[0], argsValues[1])
            }
            else -> throw UnsupportedOperationException("Unsupported number of arguments")
        }
    }

    private fun Any.toIrConst(expression: IrExpression): IrConst<*> {
        return when (this) {
            is Boolean -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Boolean, this)
            is Char -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Char, this)
            is Byte -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Byte, this)
            is Short -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Short, this)
            is Int -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Int, this)
            is Long -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Long, this)
            is String -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.String, this)
            is Float -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Float, this)
            is Double -> IrConstImpl(expression.startOffset, expression.endOffset, expression.type, IrConstKind.Double, this)
            else -> throw UnsupportedOperationException("Unsupported const element type $this")
        }
    }

    private fun <T> IrConst<T>.toPrimitive(): Primitive<T> {
        return Primitive(this)
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
        val dispatchReceiver = expression.dispatchReceiver?.accept(this, data)
        val extensionReceiver = expression.extensionReceiver?.accept(this, data)
        val newFrame = InterpreterFrame(expression.convertValueParameters(data))
        (dispatchReceiver ?: extensionReceiver)?.also { newFrame.addVar(it) }

        return if (expression.getBody() == null) {
            if (expression.symbol.owner.isFakeOverride) {
                return expression.symbol.caclOverridden(newFrame.getAll())
            }
            calculateBuiltIns(expression.symbol.descriptor, newFrame).toIrConst(expression).toPrimitive()
        } else {
            expression.getBody()!!.accept(this, newFrame)
        }
    }

    override fun <T> visitConst(expression: IrConst<T>, data: Frame): State {
        return Primitive(expression)
    }

    override fun visitConstructorCall(expression: IrConstructorCall, data: Frame): State {
        val newFrame = InterpreterFrame(expression.convertValueParameters(data))
        val state = expression.getBody()?.accept(this, newFrame) ?: unit
        return Complex(expression.symbol.descriptor.containingDeclaration, state.getState())
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall, data: Frame): State {
        if (expression.symbol.descriptor.containingDeclaration.classId?.asString() == "kotlin/Any") {
            return unit
        }

        val newFrame = InterpreterFrame(expression.convertValueParameters(data))
        val state = expression.getBody()?.accept(this, newFrame) ?: unit
        return Complex(expression.symbol.descriptor.containingDeclaration, state.getState())
    }

    private fun visitStatements(statements: List<IrStatement>, data: Frame): State {
        val state = mutableListOf<State>()
        statements.forEach {
            when (it) {
                is IrReturn -> return visitReturn(it, data)
                else -> state += it.accept(this, data)
            }
        }

        state.removeIf { it == unit }
        return when (state.size) {
            0 -> unit
            1 -> state.first()
            else -> Complex(null, state)
        }
    }

    override fun visitReturn(expression: IrReturn, data: Frame): State {
        return expression.value.accept(this, data)
    }

    override fun visitSetField(expression: IrSetField, data: Frame): State {
        val value = expression.value.accept(this, data)
        return value.setDescriptor(expression.symbol.owner.descriptor)
    }

    override fun visitGetField(expression: IrGetField, data: Frame): State {
        return data.getVar(expression.symbol.descriptor.containingDeclaration)
            .getState()
            .first { it.getDescriptor() == expression.descriptor }
            .copy()
    }

    override fun visitGetValue(expression: IrGetValue, data: Frame): State {
        if (expression.symbol.descriptor is ReceiverParameterDescriptor) {
            return data.getVar(expression.symbol.descriptor.containingDeclaration).copy()
        }
        return data.getVar(expression.symbol.descriptor).copy()
    }
}
