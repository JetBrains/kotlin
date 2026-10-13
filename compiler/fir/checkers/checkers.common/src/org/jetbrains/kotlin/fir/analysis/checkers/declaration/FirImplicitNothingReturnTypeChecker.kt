/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.types.abbreviatedTypeOrSelf
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNothing

object FirImplicitNothingReturnTypeChecker : FirCallableDeclarationChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirCallableDeclaration) {
        if (declaration !is FirNamedFunction && declaration !is FirProperty) return
        if (declaration is FirProperty && declaration.symbol is FirLocalPropertySymbol) return
        if (declaration.isOverride) return
        if (declaration.origin == FirDeclarationOrigin.ScriptCustomization.ResultProperty) return
        if (declaration.symbol.hasExplicitReturnType) {
            val notDeclaredAsNothing = !declaration.returnTypeRef.coneType.abbreviatedTypeOrSelf.isNothing
            val expandedNothing = declaration.returnTypeRef.coneType.fullyExpandedType().isNothing
            if (notDeclaredAsNothing && expandedNothing) {
                @Suppress("REDUNDANT_ELSE_IN_WHEN")
                val factory = when (declaration) {
                    is FirNamedFunction -> FirErrors.ABBREVIATED_NOTHING_RETURN_TYPE
                    is FirProperty -> FirErrors.ABBREVIATED_NOTHING_PROPERTY_TYPE
                    else -> error("Should not be here")
                }
                reporter.reportOn(declaration.source, factory)
            }
            return
        }
        if (declaration.returnTypeRef.coneType.isNothing) {
            @Suppress("REDUNDANT_ELSE_IN_WHEN")
            val factory = when (declaration) {
                is FirNamedFunction -> FirErrors.IMPLICIT_NOTHING_RETURN_TYPE
                is FirProperty -> FirErrors.IMPLICIT_NOTHING_PROPERTY_TYPE
                else -> error("Should not be here")
            }
            reporter.reportOn(declaration.source, factory)
        }
    }
}
