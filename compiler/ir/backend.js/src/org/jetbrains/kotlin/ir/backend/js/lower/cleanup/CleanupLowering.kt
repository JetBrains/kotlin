/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.cleanup

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isPure
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
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
            when (statement) {
                // returnable and inlined function blocks required for sourcemaps generation, so keep them
                is IrReturnableBlock, is IrInlinedFunctionBlock -> null
                is IrStatementContainer -> statement.statements
                else -> null
            }
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
                    unreachable = it.doesNotReturn()
                    true
                }
            }
        }

        statements.clear()

        statements += newStatements
    }

    // Checks if it is safe to assume the statement doesn't return (e.g. throws an exception or loops infinitely)
    // Takes into account cases like `fun <T> foo(): T = Any() as T`, which could be used as `foo<Nothing>()` and terminate despite the call type `Nothing`.
    // Assumes that only functions with explicit return type `Nothing` do not return.
    // Also see KotlinNothingValueExceptionLowering.kt
    private fun IrStatement.doesNotReturn(): Boolean {
        if (this !is IrExpression || !type.isNothing()) return false

        var hasFakeNothingCalls = false

        acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                super.visitCall(expression)
                hasFakeNothingCalls = hasFakeNothingCalls || expression.type.isNothing() && !expression.symbol.owner.returnType.isNothing()
            }
        })

        return !hasFakeNothingCalls
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
