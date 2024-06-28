/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.native.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirCallableDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isExpect

sealed class FirNativeObjCNameCallableChecker(mppKind: MppCheckerKind) : FirCallableDeclarationChecker(mppKind) {
    object Regular : FirNativeObjCNameCallableChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
            val containingClass = context.containingDeclarations.lastOrNull() as? FirClass ?: return
            if (containingClass.isExpect) return
            check(declaration, containingClass, context, reporter)
        }
    }

    object ForExpectClass : FirNativeObjCNameCallableChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
            val containingClass = context.containingDeclarations.lastOrNull() as? FirClass ?: return
            if (!containingClass.isExpect) return
            check(declaration, containingClass, context, reporter)
        }
    }

    protected fun check(
        declaration: FirCallableDeclaration,
        containingClass: FirClass,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (declaration !is FirSimpleFunction && declaration !is FirProperty) return
        val firTypeScope = containingClass.unsubstitutedScope(context)
        FirNativeObjCNameUtilities.checkCallableMember(firTypeScope, declaration.symbol, declaration, context, reporter)
    }
}
