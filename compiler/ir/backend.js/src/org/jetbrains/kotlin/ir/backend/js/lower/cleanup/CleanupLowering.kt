/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.cleanup

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.SideEffects
import org.jetbrains.kotlin.backend.common.ir.computeEffects
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

class CleanupLowering(val context: CommonBackendContext) : BodyLoweringPass {

    private val blockRemover = BlockRemover()
    private val codeCleaner = CodeCleaner(context)

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
                // returnable blocks required for sourcemaps generation, so keep them
                is IrReturnableBlock -> null
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

private class CodeCleaner(val context: CommonBackendContext) : IrElementVisitorVoid {

    private val functionSideEffectMemoizer = mutableMapOf<IrFunctionSymbol, SideEffects>()

    private fun cleanUpStatementsSinglePass(statements: List<IrStatement>): List<IrStatement> = buildList {
        var unreachable = false
        for (statement in statements) {
            if (statement is IrFunctionAccessExpression) {
                val functionSideEffect = statement.symbol.owner.computeEffects(true, functionSideEffectMemoizer, context).let {
                    if (it == SideEffects.ALMOST_PURE_SINGLETON_CONSTRUCTOR && statement !is IrConstructorCall)
                        SideEffects.READNONE
                    else it
                }

                if (functionSideEffect <= SideEffects.READONLY) {
                    for (i in 0 until statement.valueArgumentsCount) {
                        add(statement.getValueArgument(i)!!)
                    }
                    continue
                }
            }
            val keep = when {
                statement is IrReturn -> true
                statement is IrBreakContinue -> true
                statement is IrExpression && (statement.computeEffects(
                    true,
                    functionSideEffectMemoizer
                ) <= SideEffects.READONLY) -> false // FIXME: Only do this in production mode
                unreachable -> false
                else -> {
                    unreachable = statement.doesNotReturn()
                    true
                }
            }

            if (keep)
                add(statement)
        }
    }

    private fun IrStatementContainer.cleanUpStatements() {
        var previousStatements: List<IrStatement> = emptyList()
        var hasProgress = true
        while (hasProgress) {
            val cleanedUp = cleanUpStatementsSinglePass(statements)
            hasProgress = cleanedUp != previousStatements
            previousStatements = cleanedUp
            statements.clear()
            statements += cleanedUp
        }
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
