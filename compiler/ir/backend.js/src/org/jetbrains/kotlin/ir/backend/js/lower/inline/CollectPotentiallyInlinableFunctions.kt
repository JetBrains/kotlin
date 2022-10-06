/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.DeclarationLowering
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.inline.INLINE_ONLY_ANNOTATION_FQ_NAME

class CollectSingleCallInlinableFunctions(val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction && declaration.isInlinable() && declaration.calledOnce()) {
            declaration.markAsInlinable(context)
        }

        return null
    }

    private fun IrSimpleFunction.calledOnce(): Boolean {
        return context.optimizations.functionUsages[symbol]?.size == 1
    }

    private fun IrSimpleFunction.isInlinable(): Boolean {
        return !isExternal && !isExpect && body != null && isReal
    }

}

class CollectPotentiallyInlinableFunctions(context: JsIrBackendContext) : BodyLoweringPass {
    private val transformer = CollectPotentiallyInlinableFunctionsVisitor(context)

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(transformer)
    }
}

private class CollectPotentiallyInlinableFunctionsVisitor(val context: JsIrBackendContext) : IrElementTransformerVoid() {
    override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        if (declaration !is IrSimpleFunction || !declaration.couldBeInlined()) {
            return declaration
        }

        declaration.markAsInlinable(context)

        return super.visitDeclaration(declaration)
    }

    private fun IrSimpleFunction.couldBeInlined(): Boolean {
        if (isExternal || isExpect || body == null || isFakeOverride) return false
        val singleReturnStatement = body?.statements?.singleOrNull() as? IrReturn ?: return false
        val returnedValue = singleReturnStatement.value
        return (returnedValue is IrGetValue && !returnedValue.symbol.owner.isEffectivelyExternal()) ||
                (returnedValue is IrGetField && returnedValue.receiver.isSimpleExpression() && !returnedValue.symbol.owner.isEffectivelyExternal()) ||
                (returnedValue is IrCall && !returnedValue.symbol.owner.hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME) && returnedValue.symbol != symbol && returnedValue.valueArguments.all { it is IrGetValue? })
    }

    private fun IrExpression?.isSimpleExpression(): Boolean {
        return this == null || (this is IrGetField && !symbol.owner.isEffectivelyExternal())
    }

}

private fun IrSimpleFunction.markAsInlinable(context: JsIrBackendContext) {
    if (hasAnnotation(INLINE_ONLY_ANNOTATION_FQ_NAME)) return

    val inlineOnly = context.intrinsics.inlineOnly.constructors.single()
    annotations += context.createIrBuilder(symbol).irCall(inlineOnly)
}

val IrCall.valueArguments: Sequence<IrExpression?>
    get() = sequence {
        for (position in 0 until valueArgumentsCount) {
            yield(getValueArgument(position))
        }
    }
