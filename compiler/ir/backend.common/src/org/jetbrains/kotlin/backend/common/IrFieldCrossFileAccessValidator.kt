/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Makes sure that [IrField]s are not accessed outside their containing files.
 */
internal class IrFieldCrossFileAccessValidator(
    private val file: IrFile,
    private val reportError: ReportIrValidationError,
) : IrElementVisitorVoid {
    private val parentChain = mutableListOf<IrElement>()

    override fun visitElement(element: IrElement) {
        parentChain.push(element)
        element.acceptChildrenVoid(this)
        parentChain.pop()
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression) {
        super.visitFieldAccess(expression)

        val field = expression.symbol.owner
        val containingFile = field.fileOrNull ?: return

        if (containingFile != file) {
            reportError(file, expression, "Access to a field declared in another file: ${containingFile.path}", parentChain)
        }
    }
}
