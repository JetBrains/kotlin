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

object FirNativeObjCNameCallableChecker : FirCallableDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirSimpleFunction && declaration !is FirProperty) return
        val containingClass = context.containingDeclarations.lastOrNull() as? FirClass
        if (containingClass != null) {
            val firTypeScope = containingClass.unsubstitutedScope(context)
            FirNativeObjCNameUtilities.checkCallableMember(firTypeScope, declaration.symbol, declaration, context, reporter)
        }
    }
}
