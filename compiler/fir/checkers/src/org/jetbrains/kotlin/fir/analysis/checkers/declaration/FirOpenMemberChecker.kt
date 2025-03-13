/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.processAllDeclaredCallables
import org.jetbrains.kotlin.fir.declarations.utils.isOpen
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.lexer.KtTokens

object FirOpenMemberChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.canHaveOpenMembers) return
        declaration.symbol.processAllDeclaredCallables(context.session) { memberDeclaration ->
            if (// Marking a constructor `open` is an error covered by diagnostic code WRONG_MODIFIER_TARGET
                memberDeclaration is FirConstructorSymbol
            ) return@processAllDeclaredCallables
            val source = memberDeclaration.source ?: return@processAllDeclaredCallables
            if (memberDeclaration.isOpen && !memberDeclaration.isOverride && declaration.classKind == ClassKind.ANNOTATION_CLASS ||
                memberDeclaration.hasModifier(KtTokens.OPEN_KEYWORD) && source.shouldReportOpenFromSource
            ) {
                if (declaration.classKind == ClassKind.OBJECT) {
                    reporter.reportOn(source, FirErrors.NON_FINAL_MEMBER_IN_OBJECT, context)
                } else {
                    reporter.reportOn(source, FirErrors.NON_FINAL_MEMBER_IN_FINAL_CLASS, context)
                }
            }
        }
    }

    private val KtSourceElement.shouldReportOpenFromSource: Boolean
        get() = when (kind) {
            KtRealSourceElementKind,
            KtFakeSourceElementKind.PropertyFromParameter -> true
            else -> false
        }
}
