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
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

sealed class FirNativeObjCNameCallableChecker(mppKind: MppCheckerKind) : FirCallableDeclarationChecker(mppKind) {
    object Regular : FirNativeObjCNameCallableChecker(MppCheckerKind.Platform) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirCallableDeclaration) {
            val containingClass = context.containingDeclarations.lastOrNull() as? FirClassSymbol<*> ?: return
            if (containingClass.isExpect) return
            check(declaration, containingClass)
        }
    }

    object ForExpectClass : FirNativeObjCNameCallableChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirCallableDeclaration) {
            val containingClass = context.containingDeclarations.lastOrNull() as? FirClassSymbol<*> ?: return
            if (!containingClass.isExpect) return
            check(declaration, containingClass)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    protected fun check(
        declaration: FirCallableDeclaration,
        containingClass: FirClassSymbol<*>,
    ) {
        if (declaration !is FirNamedFunction && declaration !is FirProperty) return
        val firTypeScope = containingClass.unsubstitutedScope()
        FirNativeObjCNameUtilities.checkCallableMember(firTypeScope, declaration.symbol, declaration)
    }
}
