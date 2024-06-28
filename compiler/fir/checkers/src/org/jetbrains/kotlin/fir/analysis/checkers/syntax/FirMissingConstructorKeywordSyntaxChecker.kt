/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.forEachChildOfType
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration

object FirMissingConstructorKeywordSyntaxChecker : FirDeclarationSyntaxChecker<FirConstructor, KtDeclaration>() {

    override fun isApplicable(element: FirConstructor, source: KtSourceElement): Boolean = true

    override fun checkPsi(
        element: FirConstructor,
        source: KtPsiSourceElement,
        psi: KtDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (psi is KtConstructor<*> && psi.modifierList != null && psi.getConstructorKeyword() == null) {
            reporter.reportOn(source, FirErrors.MISSING_CONSTRUCTOR_KEYWORD, context)
        }
    }

    override fun checkLightTree(
        element: FirConstructor,
        source: KtLightSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        if (source.kind !is KtRealSourceElementKind) return
        var hasModifiers = false
        var hasConstructorKeyword = false
        source.forEachChildOfType(importantNodeTypes, depth = 1) {
            if (it.elementType == KtTokens.CONSTRUCTOR_KEYWORD) {
                hasConstructorKeyword = true
            } else {
                hasModifiers = true
            }
        }
        if (hasModifiers && !hasConstructorKeyword) {
            reporter.reportOn(source, FirErrors.MISSING_CONSTRUCTOR_KEYWORD, context)
        }
    }

    private val importantNodeTypes = setOf(KtNodeTypes.MODIFIER_LIST, KtTokens.CONSTRUCTOR_KEYWORD)
}
