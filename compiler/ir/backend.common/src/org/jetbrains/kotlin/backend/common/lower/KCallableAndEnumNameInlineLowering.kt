/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.linkage.partial.reflectionTargetLinkageError
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrCallableReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrRichCallableReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.ir.util.propertyIfAccessor
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.SpecialNames

@OptIn(UnsafeDuringIrConstructionAPI::class)
class KCallableAndEnumNameInlineLowering(context: CommonBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    private val irBuiltIns = context.irBuiltIns

    override fun lower(irFile: IrFile) {
        irFile.transformChildren(this, null)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        return when {
            expression.isInterpretableKCallableNameCall(irBuiltIns) -> {
                inlineCallableName(expression) ?: visitExpression(expression)
            }
            expression.isEnumName() -> {
                inlineEnumName(expression) ?: visitExpression(expression)
            }
            else -> visitExpression(expression)
        }
    }

    private fun inlineCallableName(expression: IrCall): IrExpression? {
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
