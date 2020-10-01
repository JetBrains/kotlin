/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.renderType
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.parentClassOrNull

internal abstract class ReflectionState : State {
    override val fields: MutableList<Variable> = mutableListOf()

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? = null

    protected fun IrClass.getIrClassOfReflectionFromList(name: String): IrClass {
        val property = this.declarations.single { it.nameForIrSerialization.asString() == name } as IrProperty
        val list = property.getter!!.returnType as IrSimpleType
        return list.arguments.single().typeOrNull!!.classOrNull!!.owner
    }

    protected fun IrClass.getIrClassOfReflection(name: String): IrClass {
        val property = this.declarations.single { it.nameForIrSerialization.asString() == name } as IrProperty
        val type = property.getter!!.returnType as IrSimpleType
        return type.classOrNull!!.owner
    }

    private fun renderReceivers(dispatchReceiver: IrType?, extensionReceiver: IrType?): String {
        return buildString {
            if (dispatchReceiver != null) {
                append(dispatchReceiver.renderType())
                append(".")
            }
            val addParentheses = dispatchReceiver != null && extensionReceiver != null
            if (addParentheses) append("(")
            if (extensionReceiver != null) {
                append(extensionReceiver.renderType())
                append(".")
            }
            if (addParentheses) append(")")
        }
    }

    protected fun renderLambda(irFunction: IrFunction): String {
        val receiver = (irFunction.dispatchReceiverParameter?.type ?: irFunction.extensionReceiverParameter?.type)?.renderType()
        val arguments = irFunction.valueParameters.joinToString(prefix = "(", postfix = ")") { it.type.renderType() }
        val returnType = irFunction.returnType.renderType()
        return ("$arguments -> $returnType").let { if (receiver != null) "$receiver.$it" else it }
    }

    protected fun renderFunction(irFunction: IrFunction): String {
        val dispatchReceiver = irFunction.parentClassOrNull?.defaultType // = instanceReceiverParameter
        val extensionReceiver = irFunction.extensionReceiverParameter?.type
        val receivers = if (irFunction is IrConstructor) "" else renderReceivers(dispatchReceiver, extensionReceiver)
        val arguments = irFunction.valueParameters.joinToString(prefix = "(", postfix = ")") { it.type.renderType() }
        val returnType = irFunction.returnType.renderType()
        return "fun $receivers${irFunction.name}$arguments: $returnType"
    }

    protected fun renderProperty(property: IrProperty): String {
        val prefix = if (property.isVar) "var" else "val"
        val receivers = renderReceivers(property.getter?.dispatchReceiverParameter?.type, property.getter?.extensionReceiverParameter?.type)
        val returnType = property.getter!!.returnType.renderType()
        return "$prefix $receivers${property.name}: $returnType"
    }
}
