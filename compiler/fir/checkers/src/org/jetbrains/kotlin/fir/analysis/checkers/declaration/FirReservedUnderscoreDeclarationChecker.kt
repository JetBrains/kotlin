/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.name.SpecialNames

object FirReservedUnderscoreDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (
            declaration is FirRegularClass ||
            declaration is FirProperty && declaration.isCatchParameter != true ||
            declaration is FirTypeAlias
        ) {
            reportIfUnderscore(declaration)
        } else if (declaration is FirTypeParameter) {
            reportIfUnderscore(declaration)
            declaration.bounds.forEach {
                checkTypeRefForUnderscore(it)
            }
        } else if (declaration is FirFunction) {
            if (declaration is FirNamedFunction) {
                reportIfUnderscore(declaration)
            }

            val isSingleUnderscoreAllowed = declaration is FirAnonymousFunction || declaration is FirPropertyAccessor
            for (parameter in declaration.valueParameters) {
                reportIfUnderscore(
                    parameter,
                    isSingleUnderscoreAllowed = isSingleUnderscoreAllowed
                )
            }
        } else if (declaration is FirFile) {
            for (import in declaration.imports) {
                checkUnderscoreDiagnostics(import.aliasSource, isExpression = false)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportIfUnderscore(
        declaration: FirDeclaration,
        isSingleUnderscoreAllowed: Boolean = false
    ) {
        val declarationSource = declaration.source
        if (declarationSource != null &&
            declarationSource.kind !is KtFakeSourceElementKind &&
            (declaration as? FirProperty)?.name != SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
        ) {
            with(SourceNavigator.forElement(declaration)) {
                val rawName = declaration.getRawName()
                if (rawName?.isUnderscore == true && !(isSingleUnderscoreAllowed && rawName == "_")) {
                    reporter.reportOn(
                        declarationSource,
                        FirErrors.UNDERSCORE_IS_RESERVED
                    )
                }
            }
        }

        when (declaration) {
            is FirValueParameter -> checkTypeRefForUnderscore(declaration.returnTypeRef)
            is FirFunction -> {
                checkTypeRefForUnderscore(declaration.returnTypeRef)
                checkTypeRefForUnderscore(declaration.receiverParameter?.typeRef)
            }
            else -> {}
        }
    }
}
