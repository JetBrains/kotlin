/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.contains
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

// See old FE's [DeclarationsChecker]
object FirMemberFunctionsChecker : FirSimpleFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        val containingDeclaration = context.containingDeclarations.lastIsInstanceOrNull<FirClassSymbol<*>>() ?: return
        checkFunction(containingDeclaration, declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkFunction(
        containingDeclaration: FirClassSymbol<*>,
        function: FirNamedFunction,
    ) {
        val source = function.source ?: return
        if (source.kind is KtFakeSourceElementKind) return
        val functionSymbol = function.symbol
        // If multiple (potentially conflicting) modality modifiers are specified, not all modifiers are recorded at `status`.
        // So, our source of truth should be the full modifier list retrieved from the source.
        val modifierList = source.getModifierList()
        val hasAbstractModifier = KtTokens.ABSTRACT_KEYWORD in modifierList
        val isAbstract = function.isAbstract || hasAbstractModifier
        if (isAbstract) {
            if (containingDeclaration is FirRegularClassSymbol && !containingDeclaration.canHaveAbstractDeclaration) {
                reporter.reportOn(
                    source,
                    FirErrors.ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS,
                    functionSymbol,
                    containingDeclaration
                )
            }
            if (function.hasBody) {
                reporter.reportOn(source, FirErrors.ABSTRACT_FUNCTION_WITH_BODY, functionSymbol)
            }
        }
        val isInsideExpectClass = isInsideExpectClass(containingDeclaration)
        val isInsideExternal = isInsideExternalClass(containingDeclaration)
        val hasOpenModifier = KtTokens.OPEN_KEYWORD in modifierList
        if (!function.hasBody) {
            if (containingDeclaration.isInterface) {
                if (Visibilities.isPrivate(function.visibility)) {
                    reporter.reportOn(source, FirErrors.PRIVATE_FUNCTION_WITH_NO_BODY, functionSymbol)
                }
                if (!isInsideExpectClass && !hasAbstractModifier && hasOpenModifier) {
                    reporter.reportOn(source, FirErrors.REDUNDANT_OPEN_IN_INTERFACE)
                }
            } else if (!isInsideExpectClass && !hasAbstractModifier && !function.isExternal && !isInsideExternal) {
                reporter.reportOn(source, FirErrors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY, functionSymbol)
            }
        }
    }
}
