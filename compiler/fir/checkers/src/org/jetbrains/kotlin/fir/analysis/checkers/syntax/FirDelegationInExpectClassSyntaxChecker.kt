/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry

object FirDelegationInExpectClassSyntaxChecker : FirDeclarationSyntaxChecker<FirRegularClass, KtClassOrObject>() {

    override fun isApplicable(element: FirRegularClass, source: KtSourceElement): Boolean = element.isExpect

    override fun checkPsi(
        element: FirRegularClass,
        source: KtPsiSourceElement,
        psi: KtClassOrObject,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (superTypeRef in element.superTypeRefs) {
            val superSource = superTypeRef.source ?: continue
            val parent = superSource.psi?.parent as? KtDelegatedSuperTypeEntry ?: continue
            reporter.reportOn(KtRealPsiSourceElement(parent), FirErrors.IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS, context)
        }
    }

    override fun checkLightTree(
        element: FirRegularClass,
        source: KtLightSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (superTypeRef in element.superTypeRefs) {
            val superSource = superTypeRef.source ?: continue
            val parent = superSource.treeStructure.getParent(superSource.lighterASTNode) ?: continue
            if (parent.tokenType == KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY) {
                reporter.reportOn(
                    parent.toKtLightSourceElement(superSource.treeStructure),
                    FirErrors.IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS,
                    context
                )
            }
        }
    }
}
