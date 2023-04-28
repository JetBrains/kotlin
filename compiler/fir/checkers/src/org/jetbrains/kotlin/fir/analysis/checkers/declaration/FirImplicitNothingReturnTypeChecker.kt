/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNothing

object FirImplicitNothingReturnTypeChecker : FirCallableDeclarationChecker() {
    override fun check(declaration: FirCallableDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirSimpleFunction && declaration !is FirProperty) return
        if (declaration is FirProperty && declaration.isLocal) return
        if (declaration.isOverride) return
        if (declaration.symbol.hasExplicitReturnType) return
        if (declaration.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty) return
        if (declaration.returnTypeRef.coneType.isNothing) {
            val factory = when (declaration) {
                is FirSimpleFunction -> FirErrors.IMPLICIT_NOTHING_RETURN_TYPE
                is FirProperty -> FirErrors.IMPLICIT_NOTHING_PROPERTY_TYPE
                else -> error("Should not be here")
            }
            reporter.reportOn(declaration.source, factory, context)
        }
    }
}
