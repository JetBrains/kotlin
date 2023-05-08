/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.checker

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrStringConcatenation
import org.jetbrains.kotlin.ir.interpreter.property
import org.jetbrains.kotlin.ir.util.isSubclassOf

class IrInterpreterNameChecker : IrInterpreterChecker {
    override fun visitElement(element: IrElement, data: IrInterpreterCheckerData) = false

    override fun visitCall(expression: IrCall, data: IrInterpreterCheckerData): Boolean {
        val owner = expression.symbol.owner
        if (!data.mode.canEvaluateFunction(owner)) return false

        return expression.isKCallableNameCall(data.irBuiltIns) || expression.isEnumName()
    }

    override fun visitStringConcatenation(expression: IrStringConcatenation, data: IrInterpreterCheckerData): Boolean {
        val possibleNameCall = expression.arguments.singleOrNull() as? IrCall ?: return false
        return possibleNameCall.accept(this, data)
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

        private fun IrCall.isEnumName(): Boolean {
            val owner = this.symbol.owner
            if (owner.extensionReceiverParameter != null || owner.valueParameters.isNotEmpty()) return false
            val property = owner.property ?: return false
            return this.dispatchReceiver is IrGetEnumValue && property.name.asString() == "name"
        }
    }
}
