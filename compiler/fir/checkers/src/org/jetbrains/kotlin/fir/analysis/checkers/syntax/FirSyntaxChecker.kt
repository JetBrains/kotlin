/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirStatement

/**
 * Checker that heavily relies on source tree. So it may have different implementation for each tree variant. Subclass should either
 *
 * - implement `checkPsiOrLightTree` in an AST-agnostic manner, or
 * - implement both `checkPsi` and `checkLightTree` if it's difficult to handle PSI and LT tree in a unified way.
 */
interface FirSyntaxChecker<in D : FirElement, P : PsiElement> {

    fun checkSyntax(element: D, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = element.source ?: return
        if (!isApplicable(element, source)) return
        @Suppress("UNCHECKED_CAST")
        when (source) {
            is KtPsiSourceElement -> checkPsi(element, source, source.psi as P, context, reporter)
            is KtLightSourceElement -> checkLightTree(element, source, context, reporter)
        }
    }

    fun isApplicable(element: D, source: KtSourceElement): Boolean = true

    fun checkPsi(element: D, source: KtPsiSourceElement, psi: P, context: CheckerContext, reporter: DiagnosticReporter) {
        checkPsiOrLightTree(element, source, context, reporter)
    }

    fun checkLightTree(element: D, source: KtLightSourceElement, context: CheckerContext, reporter: DiagnosticReporter) {
        checkPsiOrLightTree(element, source, context, reporter)
    }

    /**
     *  By default psi tree should be equivalent to light tree and can be processed the same way.
     */
    fun checkPsiOrLightTree(element: D, source: KtSourceElement, context: CheckerContext, reporter: DiagnosticReporter) {}
}

abstract class FirDeclarationSyntaxChecker<in D : FirDeclaration, P : PsiElement> :
    FirDeclarationChecker<D>(MppCheckerKind.Common),
    FirSyntaxChecker<D, P> {
    final override fun check(declaration: D, context: CheckerContext, reporter: DiagnosticReporter) {
        checkSyntax(declaration, context, reporter)
    }
}

abstract class FirExpressionSyntaxChecker<in E : FirStatement, P : PsiElement> :
    FirExpressionChecker<E>(MppCheckerKind.Common),
    FirSyntaxChecker<E, P> {
    final override fun check(expression: E, context: CheckerContext, reporter: DiagnosticReporter) {
        checkSyntax(expression, context, reporter)
    }
}
