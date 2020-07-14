/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.inline

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

class RemoveInlineFunctionsWithReifiedTypeParametersLowering: DeclarationTransformer {

    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {

        if (declaration is IrFunction && declaration.isInline && declaration.typeParameters.any { it.isReified }) {
            return emptyList()
        }

        return null
    }
}

class CopyInlineFunctionBodyLowering(val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrFunction && declaration.isInline) {
            declaration.body?.let { originalBody ->
                declaration.body = context.irFactory.createBlockBody(originalBody.startOffset, originalBody.endOffset) {
                    statements += (originalBody.deepCopyWithSymbols(declaration) as IrBlockBody).statements
                }
            }
        }

        if (declaration is IrValueParameter && declaration.parent.let { it is IrFunction && it.isInline }) {
            declaration.defaultValue?.let { originalDefault ->
                declaration.defaultValue =
                    context.irFactory.createExpressionBody(originalDefault.startOffset, originalDefault.endOffset) {
                        expression = originalDefault.expression.deepCopyWithSymbols(declaration.parent)
                    }
            }
        }

        return null
    }
}
