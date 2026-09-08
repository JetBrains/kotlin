/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtImplementationDetail::class)

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

/**
 * Reports a literal glued to an identifier, a number or a keyword, as in `a foo"bar"` or `1in a`, see
 * [KtLiteralPrefixAndSuffix].
 *
 * Only the PSI tree is handled here. Under the light tree the same check is part of `KotlinLightParser.reportErrors`,
 * which already walks the whole tree in document order right after parsing, whereas this checker only gets the literal
 * and would have to climb the tree back up once per literal to reach the leaf next to it (KT-88399).
 */
object FirPrefixAndSuffixSyntaxChecker : FirExpressionSyntaxChecker<FirStatement, KtExpression>() {
    override fun isApplicable(element: FirStatement, source: KtSourceElement): Boolean =
        source is KtPsiSourceElement &&
                source.kind !is KtFakeSourceElementKind &&
                source.elementType in KtLiteralPrefixAndSuffix.literalElementTypes

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkPsi(element: FirStatement, source: KtPsiSourceElement, psi: KtExpression) {
        fun check(affix: PsiElement, prefix: Boolean) {
            if (KtLiteralPrefixAndSuffix.isProhibitedPrefixOrSuffix(affix.node.elementType)) {
                reporter.reportOn(
                    affix.toKtPsiSourceElement(),
                    if (prefix) FirSyntaxErrors.TRAILING_WHITESPACE_REQUIRED else FirSyntaxErrors.LEADING_WHITESPACE_REQUIRED
                )
            }
        }

        psi.prevLeaf()?.let { check(it, prefix = true) }
        psi.nextLeaf()?.let { check(it, prefix = false) }
    }

}
