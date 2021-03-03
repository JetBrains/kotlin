/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.lexer.KtTokens

// See old FE's [DeclarationsChecker]
object FirMemberFunctionsChecker : FirRegularClassChecker() {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        for (member in declaration.declarations) {
            if (member is FirSimpleFunction) {
                checkFunction(declaration, member, context, reporter)
            }
        }
    }

    private fun checkFunction(
        containingDeclaration: FirRegularClass,
        function: FirSimpleFunction,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val source = function.source ?: return
        if (source.kind is FirFakeSourceElementKind) return
        // If multiple (potentially conflicting) modality modifiers are specified, not all modifiers are recorded at `status`.
        // So, our source of truth should be the full modifier list retrieved from the source.
        val modifierList = with(FirModifierList) { source.getModifierList() }
        val hasAbstractModifier = modifierList?.modifiers?.any { it.token == KtTokens.ABSTRACT_KEYWORD } == true
        val isAbstract = function.isAbstract || hasAbstractModifier
        if (isAbstract) {
            if (!containingDeclaration.canHaveAbstractDeclaration) {
                reporter.report(
                    FirErrors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(source, function, containingDeclaration),
                    context
                )
            }
            if (function.hasBody) {
                reporter.report(FirErrors.ABSTRACT_FUNCTION_WITH_BODY.on(source, function), context)
            }
        }
        val isInsideExpectClass = isInsideExpectClass(containingDeclaration, context)
        val hasOpenModifier = modifierList?.modifiers?.any { it.token == KtTokens.OPEN_KEYWORD } == true
        if (!function.hasBody) {
            if (containingDeclaration.isInterface) {
                if (Visibilities.isPrivate(function.visibility)) {
                    reporter.reportOn(source, FirErrors.PRIVATE_FUNCTION_WITH_NO_BODY, function, context)
                }
                if (!isInsideExpectClass && !hasAbstractModifier && hasOpenModifier) {
                    reporter.reportOn(source, FirErrors.REDUNDANT_OPEN_IN_INTERFACE, context)
                }
            } else if (!isInsideExpectClass && !hasAbstractModifier && !function.isExternal) {
                reporter.reportOn(source, FirErrors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, function, context)
            }
        }

        checkExpectDeclarationVisibilityAndBody(function, source, reporter, context)
    }
}
