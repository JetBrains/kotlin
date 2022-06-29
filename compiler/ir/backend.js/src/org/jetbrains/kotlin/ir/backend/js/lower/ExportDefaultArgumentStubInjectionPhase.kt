/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.export.isExported
import org.jetbrains.kotlin.ir.backend.js.utils.JsAnnotations
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.copyValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.isAnnotationWithEqualFqName
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ExportDefaultArgumentStubInjectionPhase(private val context: JsIrBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        irBody.transformChildrenVoid(ExportDefaultArgumentStubInjectionTransformer(context))
    }

    private class ExportDefaultArgumentStubInjectionTransformer(private val context: JsIrBackendContext) : IrElementTransformerVoid() {
        override fun visitCall(expression: IrCall): IrExpression {
            val calledFunction = expression.symbol.owner
            return if (!calledFunction.hasDefaultArguments() || !calledFunction.isExported(context)) {
                super.visitCall(expression)
            } else {
                val stub = calledFunction.generateExportedDefaultArgumentSub()
                with(expression) {
                    IrCallImpl(
                        startOffset, endOffset, type, stub.symbol,
                        typeArgumentsCount = typeArgumentsCount,
                        valueArgumentsCount = stub.valueParameters.size,
                        origin = JsStatementOrigins.SYNTHESIZED_STATEMENT,
                        superQualifierSymbol = superQualifierSymbol
                    ).apply {
                        dispatchReceiver = expression.dispatchReceiver
                        extensionReceiver = expression.extensionReceiver
                        copyTypeArgumentsFrom(expression)
                        copyValueArgumentsFrom(expression, stub)
                    }
                }
            }
        }

        private fun IrSimpleFunction.generateExportedDefaultArgumentSub(): IrSimpleFunction {
            return generateExportedDefaultArgumentSubWithoutBody(context).apply {
                annotations = this@generateExportedDefaultArgumentSub.annotations
                    .filter { it.isExportableAnnotation() }
                    .map { it.deepCopyWithSymbols(this as? IrDeclarationParent) }
            }
        }

        private fun IrConstructorCall.isExportableAnnotation(): Boolean =
            isAnnotationWithEqualFqName(JsAnnotations.jsExportFqn) || isAnnotationWithEqualFqName(JsAnnotations.jsNameFqn)

        private fun IrSimpleFunction.hasDefaultArguments(): Boolean =
            valueParameters.any { it.defaultValue != null }
    }
}