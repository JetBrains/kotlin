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
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirContractCallBlock
import org.jetbrains.kotlin.fir.expressions.impl.FirSingleExpressionBlock
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitor
import org.jetbrains.kotlin.name.StandardClassIds

abstract class FirUnusedCheckerBase : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext)
    abstract fun isEnabled(): Boolean

    /**
     * If this function returns true, a corresponding warning should be issued using [reporter].
     * If this function returns false, the visitor continues to visit the children of [expression].
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    abstract fun reportUnusedExpressionIfNeeded(
        expression: FirExpression,
        hasSideEffects: Boolean,
        source: KtSourceElement?,
    ): Boolean

    context(context: CheckerContext, reporter: DiagnosticReporter)
    protected open fun createVisitor(): UsageVisitorBase =
        UsageVisitorBase(context, reporter)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (!isEnabled()) return
        val visitor = createVisitor()
        when (declaration) {
            // A "used" FirBlock is one that uses the last statement as an implicit return.
            // If the containing element of the block is an FirFunction or FirAnonymousInitializer, the block is unused.
            // All other FirBlocks are considered used.
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

    protected enum class UsageState {
        Used,
        Unused,
    }

    protected open inner class UsageVisitorBase(
        protected val context: CheckerContext,
        protected val reporter: DiagnosticReporter,
    ) : FirDefaultVisitor<Unit, UsageState>() {
        override fun visitElement(element: FirElement, data: UsageState) {
            if (element is FirDeclaration) return // The checker handles nested declarations.

            val source = element.source
            if (
                data == UsageState.Unused &&
                element is FirExpression &&
                source != null
            ) {
                context(context, reporter) {
                    if (reportUnusedExpressionIfNeeded(element, element.hasSideEffect(), source)) return
                }
            }

            element.acceptChildren(this, UsageState.Used)
        }

        override fun visitAnnotation(annotation: FirAnnotation, data: UsageState) {
            // Annotations can just be ignored.
        }

        override fun visitWhenExpression(whenExpression: FirWhenExpression, data: UsageState) {
            whenExpression.subjectVariable?.initializer?.accept(this, UsageState.Used)

            // See: replaceReturnTypeIfNotExhaustive
            val branchData = if (!whenExpression.usedAsExpression && !whenExpression.isExhaustive) UsageState.Unused else data
            for (branch in whenExpression.branches) {
                branch.condition.accept(this, UsageState.Used)
                if (!branch.result.isUnitBlock) {
                    branch.result.accept(this, branchData)
                }
            }
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
                val isImplicitReturn = i == lastIndex && !block.isUnitCoerced
                statement.accept(this, if (isImplicitReturn) data else UsageState.Unused)
            }
        }

        override fun visitLoop(loop: FirLoop, data: UsageState) {
            loop.condition.accept(this, UsageState.Used)
            loop.block.accept(this, UsageState.Unused)
        }
    }
}

private val FirStatement.isUnitBlock: Boolean
    get() {
        val block = this as? FirSingleExpressionBlock ?: return false
        val qualifier = block.statement as? FirResolvedQualifier ?: return false
        return qualifier.classId == StandardClassIds.Unit
    }

/**
 * Elements with side effects are those that may execute some other expressions when executed.
 * This includes functions (as they inherently are defined as having side effects), access of
 * properties with custom getters, and may other types within the FIR tree.
 *
 * Note: ***be conservative***. Indicating an [FirExpression] is side-effect-free should only be
 * done for elements, which when removed from the code, won't impact the behavior of the code.
 */
private fun FirExpression.hasSideEffect(): Boolean {
    return when (this) {
        // Literals and references that are known to be side-effect-free.
        is FirLiteralExpression,
        is FirClassReferenceExpression,
        is FirResolvedQualifier,
        is FirThisReceiverExpression,
            -> false

        // The definition of an anonymous function is side-effect-free.
        // Invoking an anonymous function has side effects, but this is performed by another FIR element.
        is FirAnonymousFunctionExpression,
            -> false

        // A smart cast has a side effect iff its original expression has a side effect.
        is FirSmartCastExpression -> {
            originalExpression.hasSideEffect()
        }

        // A callable reference is side-effect-free only if all of its receivers are side-effect-free.
        is FirCallableReferenceAccess -> {
            dispatchReceiver?.hasSideEffect() == true ||
                    extensionReceiver?.hasSideEffect() == true ||
                    explicitReceiver?.hasSideEffect() == true
        }

        // String concatenation and class access are side-effect-free if all arguments are side-effect-free.
        is FirStringConcatenationCall,
        is FirGetClassCall,
            -> {
            arguments.any { it.hasSideEffect() }
        }

        // Property access is side-effect-free if the referenced property does not have a custom getter.
        // However, this check is limited to just considering value parameters, receiver parameters, and
        // local properties without delegates as side-effect-free, to be conservative and match K1 behavior.
        is FirPropertyAccessExpression -> {
            if (source?.kind == KtFakeSourceElementKind.ReferenceInAtomicQualifiedAccess) true
            else when (val propertySymbol = calleeReference.symbol) {
                is FirValueParameterSymbol, is FirReceiverParameterSymbol -> false
                is FirLocalPropertySymbol -> propertySymbol.hasDelegate
                else -> true
            }
        }

        else -> true
    }
}
