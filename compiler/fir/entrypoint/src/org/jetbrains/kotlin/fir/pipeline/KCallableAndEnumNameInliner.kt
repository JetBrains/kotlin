/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.linkage.partial.reflectionTargetLinkageError
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.propertyIfAccessor
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrTransformer
import org.jetbrains.kotlin.name.SpecialNames

@OptIn(UnsafeDuringIrConstructionAPI::class)
class KCallableAndEnumNameInliner(private val irBuiltIns: IrBuiltIns) : IrTransformer<Nothing?>() {
    override fun visitCall(expression: IrCall, data: Nothing?): IrElement {
        return when {
            expression.isInterpretableKCallableNameCall(irBuiltIns) -> {
                inlineCallableName(expression) ?: visitExpression(expression, data)
            }
            expression.isEnumName() -> {
                inlineEnumName(expression) ?: visitExpression(expression, data)
            }
            else -> visitExpression(expression, data)
        }
    }

    private fun inlineCallableName(expression: IrCall): IrElement? {
        val callableReference = expression.dispatchReceiver
        if (callableReference !is IrCallableReference<*> && callableReference !is IrRichCallableReference<*>) return null

        val boundArgs = when (callableReference) {
            is IrCallableReference<*> -> callableReference.arguments.filterNotNull()
            is IrRichCallableReference<*> -> callableReference.boundValues.toList() // make a copy
            else -> error("Unexpected callable reference type: ${callableReference::class.simpleName}")
        }

        val owner = when (callableReference) {
            is IrCallableReference<*> -> callableReference.symbol.owner as? IrDeclarationWithName
            is IrRichCallableReference<*> -> callableReference.reflectionTargetSymbol?.owner as? IrDeclarationWithName
            else -> error("Unexpected callable reference type: ${callableReference::class.simpleName}")
        }

        val constName = owner?.name?.asString()?.toIrConst(irBuiltIns.stringType, expression.startOffset, expression.endOffset)
            ?: return null

        val boundArgsWithoutThis = boundArgs.filterNot { it is IrGetValue && it.symbol.owner.name == SpecialNames.THIS }
        if (boundArgsWithoutThis.isEmpty()) return constName

        return IrCompositeImpl(
            expression.startOffset, expression.endOffset,
            expression.type, origin = null, statements = boundArgsWithoutThis + listOf(constName)
        )
    }

    private fun inlineEnumName(expression: IrCall): IrConst? {
        val enumValue = expression.dispatchReceiver as? IrGetEnumValue ?: return null
        val enumEntry = enumValue.symbol.owner
        return enumEntry.name.asString().toIrConst(irBuiltIns.stringType, expression.startOffset, expression.endOffset)
    }

    companion object {
        fun IrCall.isInterpretableKCallableNameCall(irBuiltIns: IrBuiltIns): Boolean {
            val receiver = this.dispatchReceiver
            if (receiver !is IrCallableReference<*> && receiver !is IrRichCallableReference<*>) {
                return false
            }

            if (receiver is IrRichCallableReference<*> && receiver.reflectionTargetLinkageError != null) {
                // There was a partial linkage error of reflectionTargetSymbol -> we don't have accurate information about the callable's name.
                return false
            }

            val directMember = this.symbol.owner.propertyIfAccessor

            val irClass = directMember.parent as? IrClass ?: return false
            if (!irClass.isSubclassOf(irBuiltIns.kCallableClass.owner)) return false

            val name = when (directMember) {
                is IrSimpleFunction -> directMember.name
                is IrProperty -> directMember.name
                else -> return false
            }
            return name.asString() == "name"
        }

        fun IrCall.isEnumName(): Boolean {
            val owner = this.symbol.owner
            if (!owner.hasShape(dispatchReceiver = true, regularParameters = 0)) return false
            val property = owner.correspondingPropertySymbol?.owner ?: return false
            return this.dispatchReceiver is IrGetEnumValue && property.name.asString() == "name"
        }
    }
}
