/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirAnonymousInitializer
import org.jetbrains.kotlin.fir.declarations.FirCodeFragment
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirElvisExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirLoop
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.FirTryExpression
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor


abstract class FirUnusedCheckerBase : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    abstract fun isEnabled(context: CheckerContext): Boolean

    /**
     * Checks whether a given expression is considered unused.
     *
     * If this function returns true, a corresponding warning should be issued using [reporter].
     * If this function returns false, the visitor continues to visit the children of [expression].
     */
    abstract fun isExpressionUnused(
        expression: FirExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter,
        source: KtSourceElement?,
    ): Boolean

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!isEnabled(context)) return
        val visitor = UsageVisitor(context, reporter)
        when (declaration) {
            // A "used" FirBlock is one that uses the last statement as an implicit return.
            // If the containing element of the block is an FirFunction or FirAnonymousInitializer, the block is unused.
            // All other FirBlocks are considered used.
            is FirReplSnippet -> declaration.body.acceptChildren(visitor, UsageState.Used)
            is FirCodeFragment -> declaration.block.acceptChildren(visitor, UsageState.Used)
            is FirAnonymousInitializer -> declaration.body?.acceptChildren(visitor, UsageState.Unused)
            is FirFunction -> {
                val lastStatementUsed = declaration is FirAnonymousFunction && declaration.isLambda
                declaration.body?.accept(visitor, if (lastStatementUsed) UsageState.Used else UsageState.Unused)
            }

            // Variables are special as they have an FirExpression as the entry point, which is always used.
            is FirVariable -> {
                declaration.initializer?.accept(visitor, UsageState.Used)
                declaration.delegate?.accept(visitor, UsageState.Used)
            }

            else -> {} // Other declarations are not checked.
        }
    }

    private enum class UsageState {
        Used,
        Unused,
    }

    private inner class UsageVisitor(
        private val context: CheckerContext,
        private val reporter: DiagnosticReporter,
    ) : FirDefaultVisitor<Unit, UsageState>() {
        override fun visitElement(element: FirElement, data: UsageState) {
            if (element is FirDeclaration) return // The checker handles nested declarations.

            val source = element.source
            if (
                data == UsageState.Unused &&
                element is FirExpression &&
                source != null
            ) {
                if (isExpressionUnused(element, context, reporter, source)) return
            }

            element.acceptChildren(this, UsageState.Used)
        }

        override fun visitAnnotation(annotation: FirAnnotation, data: UsageState) {
            // Annotations can just be ignored.
        }

        override fun visitWhenExpression(whenExpression: FirWhenExpression, data: UsageState) {
            when (val variable = whenExpression.subjectVariable) {
                null -> whenExpression.subject?.accept(this, UsageState.Used)
                else -> variable.accept(this, UsageState.Used)
            }
            for (branch in whenExpression.branches) {
                branch.condition.accept(this, UsageState.Used)
                branch.result.accept(this, data)
            }
        }

        override fun visitElvisExpression(elvisExpression: FirElvisExpression, data: UsageState) {
            elvisExpression.lhs.accept(this, data)
            elvisExpression.rhs.accept(this, data)
        }

        override fun visitSafeCallExpression(safeCallExpression: FirSafeCallExpression, data: UsageState) {
            safeCallExpression.selector.accept(this, data)
        }

        override fun visitCheckNotNullCall(checkNotNullCall: FirCheckNotNullCall, data: UsageState) {
            checkNotNullCall.argument.accept(this, data)
        }

        override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: UsageState) {
            typeOperatorCall.arguments.forEach { it.accept(this, data) }
        }

        override fun visitTryExpression(tryExpression: FirTryExpression, data: UsageState) {
            tryExpression.tryBlock.accept(this, data)
            for (catch in tryExpression.catches) {
                catch.block.accept(this, data)
            }
            tryExpression.finallyBlock?.accept(this, UsageState.Unused)
        }

        override fun visitBlock(block: FirBlock, data: UsageState) {
            // Increment and decrement operators are always considered used (because they have a side effect).
            if (block.source?.kind is KtFakeSourceElementKind.DesugaredIncrementOrDecrement) return
            // Function contract blocks can be ignored.
            if (block is FirContractCallBlock) return

            val statements = block.statements
            val lastIndex = statements.lastIndex
            for (i in statements.indices) {
                val statement = statements[i]
                val isImplicitReturn = i == lastIndex &&
                        statement is FirExpression &&
                        statement.resolvedType.isSubtypeOf(block.resolvedType, context.session)
                statement.accept(this, if (isImplicitReturn) data else UsageState.Unused)
            }
        }

        override fun visitLoop(loop: FirLoop, data: UsageState) {
            loop.condition.accept(this, UsageState.Used)
            loop.block.accept(this, UsageState.Unused)
        }
    }
}
