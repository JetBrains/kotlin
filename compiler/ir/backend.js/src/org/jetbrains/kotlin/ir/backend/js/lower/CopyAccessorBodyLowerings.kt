/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

// Copies property accessors and initializers so that the PropertyAccessorInilineLowering may access them safely.
class CopyAccessorBodyLowerings(private val context: CommonBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration is IrSimpleFunction && declaration.correspondingPropertySymbol != null) {
            declaration.body?.let { originalBody ->
                declaration.body = context.irFactory.createBlockBody(originalBody.startOffset, originalBody.endOffset) {
                    statements += (originalBody.deepCopyWithSymbols(declaration) as IrBlockBody).statements
                }
            }
        }

        if (declaration is IrField) {
            declaration.initializer?.let { originalBody ->
                declaration.initializer = context.irFactory.createExpressionBody(
                    startOffset = originalBody.startOffset,
                    endOffset = originalBody.endOffset,
                    expression = originalBody.expression.deepCopyWithSymbols(declaration),
                )
            }
        }

        return null
    }
}