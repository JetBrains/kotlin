/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextInterface
import org.jetbrains.kotlin.ir.builders.constTrue
import org.jetbrains.kotlin.ir.builders.eqeqeq
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

/**
 * This lowering pass replaces `eqeq` [IrExpression]s with `object` instances operands with
 *
 * - `true`, if both left and right operands are same object instances,
 * - `eqeqeq`, if either left or right operand is an object instance.
 * - `eqeqeq`, if the left operand is an instance of a sealed class with no subclasses containing user-defined `equals` function and the
 * right one is an object instance, which is subclass of that sealed class.
 *
 * Nothing is changed if `equals` is overridden in the object. Nothing is changed if the object is declared in another module.
 */
class ObjectInstanceEqualsLowering(val context: CommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) = irBody.transformChildrenVoid(ObjectInstanceEqualsTransformer(this))
}

private val EQUALS_IDENTIFIER = Name.identifier("equals")

private fun IrFunction.isEqualsInheritedFromAny() = name == EQUALS_IDENTIFIER &&
        dispatchReceiverParameter != null &&
        valueParameters.size == 1 &&
        valueParameters[0].type.isNullableAny()


private class ObjectInstanceEqualsTransformer(val lower: ObjectInstanceEqualsLowering) : IrElementTransformerVoid(),
    IrGeneratorContextInterface {

    override val irBuiltIns: IrBuiltIns
        get() = lower.context.irBuiltIns

    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol != irBuiltIns.eqeqSymbol) return super.visitCall(expression)
        return handleObject(expression) ?: handleSealedClass(expression) ?: super.visitCall(expression)
    }

    private fun handleObject(expression: IrCall): IrExpression? {
        val left = expression.getValueArgument(0) ?: return null
        val right = expression.getValueArgument(1) ?: return null
        if (left !is IrGetObjectValue) return null
        val obj = left.symbol.owner
        if (obj.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB) return null
        if (obj.declarations.find { it is IrFunction && it.isEqualsInheritedFromAny() }?.isFakeOverride != true) return null
        if (hasUserDefinedEquals(obj)) return null
        if (right is IrGetObjectValue && right.symbol == left.symbol) return constTrue(expression.startOffset, expression.endOffset)
        return eqeqeq(expression.startOffset, expression.endOffset, left, right)
    }

    private fun hasUserDefinedEquals(cls: IrClass) = (cls.superTypes - irBuiltIns.anyType)
        .mapNotNull(IrType::getClass)
        .flatMap(IrClass::declarations)
        .any { decl -> decl is IrFunction && decl.isEqualsInheritedFromAny() && !decl.isFakeOverride }

    private fun deepSealedSubclasses(cls: IrClass): List<IrClassSymbol> {
        val stack = ArrayDeque<IrClass>()
        var curr: IrClass? = cls
        val res = mutableListOf<IrClassSymbol>()

        while (curr != null || stack.isNotEmpty()) {
            curr?.sealedSubclasses?.forEach {
                stack.addLast(it.owner)
                res.add(it)
            }

            curr = stack.removeLastOrNull()
        }

        return res
    }

    private fun handleSealedClass(expression: IrCall): IrExpression? {
        val left = expression.getValueArgument(0) ?: return null
        val right = expression.getValueArgument(1) ?: return null
        val cls = left.type.getClass() ?: return null
        if (cls.modality != Modality.SEALED) return null
        if (cls.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB) return null
        val sealedSubclasses = deepSealedSubclasses(cls).map(IrClassSymbol::owner)
        if (sealedSubclasses.any(::hasUserDefinedEquals)) return null
        if (right !is IrGetObjectValue) return null
        if (cls !in right.symbol.owner.superTypes.mapNotNull(IrType::getClass)) return null
        return eqeqeq(expression.startOffset, expression.endOffset, left, right)
    }
}
