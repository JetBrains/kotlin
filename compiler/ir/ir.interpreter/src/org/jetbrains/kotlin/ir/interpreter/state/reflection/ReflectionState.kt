/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.stack.Fields
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.render
import kotlin.math.min

internal abstract class ReflectionState : State {
    override val fields: Fields = mutableMapOf()

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? = null

    private fun renderReceivers(function: IrFunction, dispatchReceiverTypeOverride: IrType? = null): String {
        val dispatchReceiver = function.dispatchReceiverParameter?.let { dispatchReceiverTypeOverride ?: it.type }
        val extensionReceiver = function.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type
        return buildString {
            if (dispatchReceiver != null) {
                append(dispatchReceiver.renderType()).append(".")
            }

            if (extensionReceiver != null) {
                val addParentheses = dispatchReceiver != null
                if (addParentheses) append("(")
                append(extensionReceiver.renderType()).append(".")
                if (addParentheses) append(")")
            }
        }
    }

    private fun renderNonReceiverParameters(irFunction: IrFunction, renderEmptyParenthesis: Boolean = true): String {
        val nonReceivers = irFunction.parameters
            .filter { it.kind == IrParameterKind.Context || it.kind == IrParameterKind.Regular }
        if (nonReceivers.isEmpty() && !renderEmptyParenthesis)
            return ""
        return nonReceivers.joinToString(prefix = "(", postfix = ")") { it.type.renderType() }
    }

    protected fun renderLambda(irFunction: IrFunction): String {
        val receivers = renderReceivers(irFunction)
        val otherParameters = renderNonReceiverParameters(irFunction)
        val returnType = irFunction.returnType.renderType()
        return "$receivers$otherParameters -> $returnType"
    }

    protected fun renderFunction(irFunction: IrFunction): String {
        val receivers = renderReceivers(irFunction, dispatchReceiverTypeOverride = irFunction.parentClassOrNull?.defaultType)
        val otherParameters = renderNonReceiverParameters(irFunction)
        val returnType = irFunction.returnType.renderType()
        return "fun $receivers${irFunction.name}$otherParameters: $returnType"
    }

    protected fun renderProperty(property: IrProperty): String {
        val prefix = if (property.isVar) "var" else "val"
        val getter = property.getter!!
        val receivers = renderReceivers(getter)
        val contextParameters = renderNonReceiverParameters(getter, renderEmptyParenthesis = false)
        val returnType = getter.returnType.renderType()
        return "$prefix $receivers$contextParameters${property.name}: $returnType"
    }

    protected fun IrType.renderType(): String {
        var renderedType = this.render().replace("<root>.", "")
        if (renderedType.contains("<get-")) {
            val startIndex = renderedType.indexOf("<get-")
            val lastTriangle = renderedType.indexOf('>', startIndex) + 1
            renderedType = renderedType.replaceRange(startIndex, lastTriangle, "get")
        }
        do {
            val index = renderedType.indexOf(" of ")
            if (index == -1) break
            val replaceUntilComma = renderedType.indexOf(',', index)
            val replaceUntilTriangle = renderedType.indexOf('>', index)
            val replaceUntil = when {
                replaceUntilComma == -1 && replaceUntilTriangle == -1 -> renderedType.length
                replaceUntilComma == -1 -> replaceUntilTriangle
                replaceUntilTriangle == -1 -> replaceUntilComma
                else -> min(replaceUntilComma, replaceUntilTriangle)
            }
            renderedType = renderedType.replaceRange(index, replaceUntil, "")
        } while (true)
        return renderedType
    }
}
