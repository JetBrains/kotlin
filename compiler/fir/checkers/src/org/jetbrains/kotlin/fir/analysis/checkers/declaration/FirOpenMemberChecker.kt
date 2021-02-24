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
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirModifierList.Companion.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirCallableMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.isOpen
import org.jetbrains.kotlin.lexer.KtTokens

object FirOpenMemberChecker : FirClassChecker() {
    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.canHaveOpenMembers) return
        for (memberDeclaration in declaration.declarations) {
            if (memberDeclaration !is FirCallableMemberDeclaration<*> ||
                // Marking a constructor `open` is an error covered by diagnostic code WRONG_MODIFIER_TARGET
                memberDeclaration is FirConstructor
            ) continue
            val source = memberDeclaration.source ?: continue
            if (memberDeclaration.isOpen || (source.hasOpenModifierInSource && source.shouldReportOpenFromSource)) {
                if (declaration.classKind == ClassKind.OBJECT) {
                    reporter.reportOn(source, FirErrors.NON_FINAL_MEMBER_IN_OBJECT, context)
                } else {
                    reporter.reportOn(source, FirErrors.NON_FINAL_MEMBER_IN_FINAL_CLASS, context)
                }
            }
        }
    }

    private val FirSourceElement.hasOpenModifierInSource: Boolean get() = getModifierList()?.modifiers?.any { it.token == KtTokens.OPEN_KEYWORD } == true
    private val FirSourceElement.shouldReportOpenFromSource: Boolean
        get() = when (kind) {
            FirRealSourceElementKind,
            FirFakeSourceElementKind.PropertyFromParameter -> true
            else -> false
        }
}
