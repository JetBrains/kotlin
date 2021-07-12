/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirRealSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.utils.isOpen
import org.jetbrains.kotlin.lexer.KtTokens

object FirOpenMemberChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.canHaveOpenMembers) return
        for (memberDeclaration in declaration.declarations) {
            if (memberDeclaration !is FirCallableDeclaration ||
                // Marking a constructor `open` is an error covered by diagnostic code WRONG_MODIFIER_TARGET
                memberDeclaration is FirConstructor
            ) continue
            val source = memberDeclaration.source ?: continue
            if (memberDeclaration.isOpen || memberDeclaration.hasModifier(KtTokens.OPEN_KEYWORD) && source.shouldReportOpenFromSource) {
                if (declaration.classKind == ClassKind.OBJECT) {
                    reporter.reportOn(source, FirErrors.NON_FINAL_MEMBER_IN_OBJECT, context)
                } else {
                    reporter.reportOn(source, FirErrors.NON_FINAL_MEMBER_IN_FINAL_CLASS, context)
                }
            }
        }
    }

    private val FirSourceElement.shouldReportOpenFromSource: Boolean
        get() = when (kind) {
            FirRealSourceElementKind,
            FirFakeSourceElementKind.PropertyFromParameter -> true
            else -> false
        }
}
