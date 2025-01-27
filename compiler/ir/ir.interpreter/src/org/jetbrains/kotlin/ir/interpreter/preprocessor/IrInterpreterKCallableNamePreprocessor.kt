/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.preprocessor

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.interpreter.property
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.SpecialNames

// Note: this class still will not allow us to evaluate things like `A()::a.name + `A()::b.name`.
// This code will be optimized but not completely turned into "ab" result.
class IrInterpreterKCallableNamePreprocessor : IrInterpreterPreprocessor() {
    override fun visitCall(expression: IrCall, data: IrInterpreterPreprocessorData): IrElement {
        if (!expression.isKCallableNameCall(data.irBuiltIns)) return super.visitCall(expression, data)
        return handleCallableReference(expression, data)
    }

    private fun handleCallableReference(expression: IrCall, data: IrInterpreterPreprocessorData): IrElement {
        val callableReference = expression.dispatchReceiver
        val boundArgs = when (callableReference) {
            is IrCallableReference<*> -> callableReference.arguments.filterNotNull()
            is IrRichPropertyReference -> callableReference.boundValues.toList() // make a copy
            is IrRichFunctionReference -> callableReference.boundValues.toList() // make a copy
            else -> return super.visitCall(expression, data)
        }

        // Transform reference from bound to unbound one
        val typeArguments = (callableReference.type as IrSimpleType).arguments.map { it.typeOrNull!! }
        if (boundArgs.isNotEmpty() && callableReference.type.isKFunction()) {
            val newTypeArgs = when (callableReference) {
                is IrCallableReference<*> -> {
                    val typeArgIterator = typeArguments.iterator()
                    callableReference.arguments
                        .mapTo(mutableListOf()) { it?.type ?: typeArgIterator.next() }
                        .also { it.add(typeArgIterator.next()) /* add the return type */ }
                }
                else -> boundArgs.map { it.type } + typeArguments
            }

            val kFunction = data.irBuiltIns.kFunctionN(typeArguments.size)
            callableReference.type = kFunction.typeWith(*newTypeArgs.toTypedArray<IrType>())
        }

        // We want to change symbol to keep IR correct. If something goes wrong during interpretation, we still will have compilable code.
        expression.symbol = data.irBuiltIns.kCallableClass.owner.properties.single { it.name.asString() == "name" }.getter!!.symbol

        // Callable reference shouldn't have any bound arguments
        when (callableReference) {
            is IrCallableReference<*> -> callableReference.arguments.fill(null)
            is IrRichPropertyReference -> callableReference.boundValues.clear()
            is IrRichFunctionReference -> callableReference.boundValues.clear()
            else -> return super.visitCall(expression, data)
        }

        val boundArgsWithoutThis = boundArgs.filterNot { it is IrGetValue && it.symbol.owner.name == SpecialNames.THIS }
        if (boundArgsWithoutThis.isEmpty()) return expression

        return IrCompositeImpl(
            expression.startOffset, expression.endOffset,
            expression.type, origin = null, statements = boundArgsWithoutThis + listOf(expression)
        )
    }

    companion object {
        fun IrCall.isKCallableNameCall(irBuiltIns: IrBuiltIns): Boolean {
            val receiver = this.dispatchReceiver
            if (receiver !is IrCallableReference<*> && receiver !is IrRichPropertyReference && receiver !is IrRichFunctionReference) {
                return false
            }

            val directMember: IrOverridableMember = this.symbol.owner.let { it.property ?: it }

            val irClass = directMember.parent as? IrClass ?: return false
            if (!irClass.isSubclassOf(irBuiltIns.kCallableClass.owner)) return false

            val name = when (directMember) {
                is IrSimpleFunction -> directMember.name
                is IrProperty -> directMember.name
            }
            return name.asString() == "name"
        }

        fun IrCall.isEnumName(): Boolean {
            val owner = this.symbol.owner
            if (!owner.hasShape(dispatchReceiver = true, regularParameters = 0)) return false
            val property = owner.property ?: return false
            return this.dispatchReceiver is IrGetEnumValue && property.name.asString() == "name"
        }
    }
}