/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.cleanup

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.utils.isPure
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class CleanupLowering : BodyLoweringPass {

    private val blockRemover = BlockRemover()
    private val codeCleaner = CodeCleaner()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        // TODO: merge passes together
        irBody.acceptVoid(blockRemover)
        irBody.acceptVoid(codeCleaner)
    }
}

private class BlockRemover : IrElementVisitorVoid {
    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    private fun process(container: IrStatementContainer) {
        container.statements.transformFlat { statement ->
            (statement as? IrStatementContainer)?.statements
        }
    }

    override fun visitBlockBody(body: IrBlockBody) {
        super.visitBlockBody(body)

        process(body)
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        super.visitContainerExpression(expression)

        process(expression)
    }
}

private class CodeCleaner : IrElementVisitorVoid {

    private fun IrStatementContainer.cleanUpStatements() {
        var unreachable = false

        val newStatements = statements.filter {
            when {
                unreachable -> false
                it is IrExpression && it.isPure(true) -> false
                else -> {
                    unreachable = it is IrExpression && it.type.isNothing()
                    true
                }
            }
        }

        statements.clear()

        statements += newStatements
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitBlockBody(body: IrBlockBody) {
        super.visitBlockBody(body)
        body.cleanUpStatements()
    }

    override fun visitContainerExpression(expression: IrContainerExpression) {
        super.visitContainerExpression(expression)
        expression.cleanUpStatements()
    }
}