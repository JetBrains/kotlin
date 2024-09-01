/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.isInlineParameter
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * This lowering is a hack for JVM IR inliner. Two reasons why we need it:
 * 1. We need to traverse defaults of inline function and also transform them into INLINE_LAMBDA.
 * But if these transformations remain, JVM backend will complain that it can't handle function reference.
 * References marked as INLINE_LAMBDA are not converted into classes.
 * 2. Kind of the same problem but with SUSPEND_CONVERSION.
 * Must roll back changes so these references could be transformed into classes.
 */
@PhaseDescription(
    name = "RestoreInlineLambda",
    description = "Traverse INLINE_LAMBDA blocks and restore original expression before converting it into inline lambda." +
            "This is required because JVM will not convert these lambdas into classes otherwise.",
)
class RestoreInlineLambda(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    override fun lower(irFile: IrFile) {
        if (context.config.enableIrInliner) {
            irFile.transform(this, null)
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.isInline) {
            for (parameter in declaration.valueParameters) {
                if (parameter.isInlineParameter()) {
                    val lambda = parameter.defaultValue?.expression as? IrBlock ?: continue
                    val function = lambda.statements.first() as IrFunction
                    val reference = lambda.statements.last() as IrFunctionReference
                    val original = reference.attributeOwnerId as IrExpression

                    if (original is IrFunctionExpression) {
                        function.origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                        reference.origin = original.origin
                    } else if (original is IrCallableReference<*>) {
                        parameter.defaultValue?.expression = original
                    }
                }
            }
        }

        return super.visitFunction(declaration)
    }

    override fun visitBlock(expression: IrBlock): IrExpression {
        if (expression.origin == IrStatementOrigin.SUSPEND_CONVERSION) {
            val function = expression.statements.first() as IrFunction
            function.origin = IrDeclarationOrigin.ADAPTER_FOR_SUSPEND_CONVERSION

            val possibleReference = expression.statements.last()
            val reference = if (possibleReference is IrTypeOperatorCall) possibleReference.argument else possibleReference
            (reference as IrFunctionReference).origin = expression.origin
        }
        return super.visitBlock(expression)
    }
}