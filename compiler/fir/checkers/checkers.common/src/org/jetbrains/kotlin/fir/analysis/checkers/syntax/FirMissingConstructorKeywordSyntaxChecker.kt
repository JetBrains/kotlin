/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirConstructorChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.forEachChildOfType
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.syntax.withSyntax
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds
import org.jetbrains.kotlin.syntax.SyntaxIdSet

object FirMissingConstructorKeywordSyntaxChecker : FirConstructorChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirConstructor) {
        val source = declaration.source ?: return
        if (source.kind !is KtRealSourceElementKind) return

        var hasModifiers = false
        var hasConstructorKeyword = false
        withSyntax {
            source.forEachChildOfType(importantNodeTypes, depth = 1) {
                if (it.syntaxId == KotlinSyntaxTokenIds.CONSTRUCTOR_KEYWORD) {
                    hasConstructorKeyword = true
                } else {
                    hasModifiers = true
                }
            }
        }
        if (hasModifiers && !hasConstructorKeyword) {
            reporter.reportOn(source, FirErrors.MISSING_CONSTRUCTOR_KEYWORD)
        }
    }

    private val importantNodeTypes = SyntaxIdSet.of(KotlinSyntaxNodeTypeIds.MODIFIER_LIST, KotlinSyntaxTokenIds.CONSTRUCTOR_KEYWORD)
}
