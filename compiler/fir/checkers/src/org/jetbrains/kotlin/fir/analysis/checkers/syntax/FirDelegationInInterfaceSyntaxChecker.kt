/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry

object FirDelegationInInterfaceSyntaxChecker : FirDeclarationSyntaxChecker<FirRegularClass, KtClass>() {

    override fun isApplicable(element: FirRegularClass, source: KtSourceElement): Boolean = element.isInterface

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkPsi(
        element: FirRegularClass,
        source: KtPsiSourceElement,
        psi: KtClass,
    ) {
        for (superTypeRef in element.superTypeRefs) {
            val superSource = superTypeRef.source ?: continue
            if (superSource.psi?.parent is KtDelegatedSuperTypeEntry) {
                reporter.reportOn(superSource, FirErrors.DELEGATION_IN_INTERFACE)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkLightTree(
        element: FirRegularClass,
        source: KtLightSourceElement,
    ) {
        for (superTypeRef in element.superTypeRefs) {
            val superSource = superTypeRef.source ?: continue
            if (superSource.treeStructure.getParent(superSource.lighterASTNode)?.tokenType == KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY) {
                reporter.reportOn(superSource, FirErrors.DELEGATION_IN_INTERFACE)
            }
        }
    }
}
