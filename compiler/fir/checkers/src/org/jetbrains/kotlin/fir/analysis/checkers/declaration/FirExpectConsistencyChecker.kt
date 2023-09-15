/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.hasBody
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall

// See old FE's [DeclarationsChecker]
object FirExpectConsistencyChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = declaration.source ?: return
        if (source.kind is KtFakeSourceElementKind) return

        val isTopLevel = context.containingDeclarations.size == 1
        val lastClass = context.containingDeclarations.lastOrNull() as? FirClass
        val isTopLevelOrInsideClass = isTopLevel || lastClass != null

        if (declaration is FirAnonymousInitializer) {
            if (lastClass?.isExpect == true) {
                reporter.reportOn(source, FirErrors.EXPECTED_DECLARATION_WITH_BODY, context)
            }
            return
        }

        if (declaration !is FirMemberDeclaration || !declaration.isExpect) {
            return
        }

        getConstructorDelegationCall(declaration)?.let { delegatedConstructor ->
            reporter.reportOn(delegatedConstructor.source, FirErrors.EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL, context)
        }

        if (isProhibitedPrivateDeclaration(declaration, isTopLevelOrInsideClass)) {
            reporter.reportOn(source, FirErrors.EXPECTED_PRIVATE_DECLARATION, context)
        }

        if (isProhibitedDeclarationWithBody(declaration)) {
            reporter.reportOn(source, FirErrors.EXPECTED_DECLARATION_WITH_BODY, context)
        }
    }

    private fun getConstructorDelegationCall(declaration: FirMemberDeclaration): FirDelegatedConstructorCall? {
        if (declaration is FirConstructor) {
            if (!declaration.isPrimary) {
                val delegatedConstructor = declaration.delegatedConstructor
                val delegatedConstructorSource = delegatedConstructor?.source
                if (delegatedConstructorSource?.kind !is KtFakeSourceElementKind) {
                    return delegatedConstructor
                }
            }
        }
        return null
    }

    private fun isProhibitedPrivateDeclaration(declaration: FirMemberDeclaration, isTopLevelOrInsideClass: Boolean): Boolean {
        return isTopLevelOrInsideClass && declaration !is FirConstructor && Visibilities.isPrivate(declaration.visibility)
    }

    private fun isProhibitedDeclarationWithBody(declaration: FirMemberDeclaration): Boolean {
        return declaration is FirFunction && declaration.hasBody
    }
}
