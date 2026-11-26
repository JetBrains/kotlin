/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirStatement

/**
 * Checker that heavily relies on the source tree.
 * Subclasses must stay independent of a concrete tree: anything tree-specific belongs to [org.jetbrains.kotlin.fir.syntax.FirSyntaxComponent].
 */
interface FirSyntaxChecker<D : FirElement> {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun check(element: D, source: KtSourceElement)
}

abstract class FirDeclarationSyntaxChecker<D : FirDeclaration> :
    FirDeclarationChecker<D>(MppCheckerKind.Common),
    FirSyntaxChecker<D> {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    final override fun check(declaration: D) {
        val source = declaration.source ?: return
        check(declaration, source)
    }
}

abstract class FirExpressionSyntaxChecker<E : FirStatement> :
    FirExpressionChecker<E>(MppCheckerKind.Common),
    FirSyntaxChecker<E> {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    final override fun check(expression: E) {
        val source = expression.source ?: return
        check(expression, source)
    }
}
