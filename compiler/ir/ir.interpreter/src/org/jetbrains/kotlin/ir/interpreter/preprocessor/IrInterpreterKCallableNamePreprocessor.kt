/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.preprocessor

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.interpreter.property
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.isKFunction
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.SpecialNames

// Note: this class still will not allow us to evaluate things like `A()::a.name + `A()::b.name`.
// This code will be optimized but not completely turned into "ab" result.
class IrInterpreterKCallableNamePreprocessor : IrInterpreterPreprocessor {
    override fun visitCall(expression: IrCall, data: IrInterpreterPreprocessorData): IrElement {
        if (!expression.isKCallableNameCall(data.irBuiltIns)) return super.visitCall(expression, data)

        val callableReference = expression.dispatchReceiver as? IrCallableReference<*> ?: return super.visitCall(expression, data)

        // receiver is needed for bound callable reference
        val receiver = callableReference.dispatchReceiver ?: callableReference.extensionReceiver ?: return expression

        val typeArguments = (callableReference.type as IrSimpleType).arguments.map { it.typeOrNull!! }
        if (callableReference.type.isKFunction()) {
            val kFunction = data.irBuiltIns.kFunctionN(typeArguments.size)
            val newType = kFunction.typeWith(receiver.type, *typeArguments.toTypedArray())
            callableReference.type = newType
        }

        // We want to change symbol to keep IR correct. If something goes wrong during interpretation, we still will have compilable code.
        expression.symbol = data.irBuiltIns.kCallableClass.owner.properties.single { it.name.asString() == "name" }.getter!!.symbol

        callableReference.dispatchReceiver = null
        callableReference.extensionReceiver = null
        if (receiver is IrGetValue && receiver.symbol.owner.name == SpecialNames.THIS) return expression

        return IrCompositeImpl(
            expression.startOffset, expression.endOffset, expression.type, origin = null, statements = listOf(receiver, expression)
        )
    }

    companion object {
        fun IrCall.isKCallableNameCall(irBuiltIns: IrBuiltIns): Boolean {
            if (this.dispatchReceiver !is IrCallableReference<*>) return false

            val directMember = this.symbol.owner.let { it.property ?: it }

            val irClass = directMember.parent as? IrClass ?: return false
            if (!irClass.isSubclassOf(irBuiltIns.kCallableClass.owner)) return false

            val name = when (directMember) {
                is IrSimpleFunction -> directMember.name
                is IrProperty -> directMember.name
                else -> throw AssertionError("Should be IrSimpleFunction or IrProperty, got $directMember")
            }
            return name.asString() == "name"
        }

        fun IrCall.isEnumName(): Boolean {
            val owner = this.symbol.owner
            if (owner.extensionReceiverParameter != null || owner.valueParameters.isNotEmpty()) return false
            val property = owner.property ?: return false
            return this.dispatchReceiver is IrGetEnumValue && property.name.asString() == "name"
        }
    }
}