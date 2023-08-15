/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.SourceNavigator
import org.jetbrains.kotlin.fir.analysis.checkers.checkUnderscoreDiagnostics
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isUnderscore
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.isCatchParameter
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.name.SpecialNames

object FirReservedUnderscoreDeclarationChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (
            declaration is FirRegularClass ||
            declaration is FirTypeParameter ||
            declaration is FirProperty && declaration.isCatchParameter != true ||
            declaration is FirTypeAlias
        ) {
            reportIfUnderscore(declaration, context, reporter)
        } else if (declaration is FirFunction) {
            if (declaration is FirSimpleFunction) {
                reportIfUnderscore(declaration, context, reporter)
            }

            val isSingleUnderscoreAllowed = declaration is FirAnonymousFunction || declaration is FirPropertyAccessor
            for (parameter in declaration.valueParameters) {
                reportIfUnderscore(
                    parameter,
                    context,
                    reporter,
                    isSingleUnderscoreAllowed = isSingleUnderscoreAllowed
                )
            }
        } else if (declaration is FirFile) {
            for (import in declaration.imports) {
                checkUnderscoreDiagnostics(import.aliasSource, context, reporter, isExpression = false)
            }
        }
    }

    private fun reportIfUnderscore(
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
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
                        FirErrors.UNDERSCORE_IS_RESERVED,
                        context
                    )
                }
            }
        }

        val returnOrReceiverTypeRef = when (declaration) {
            is FirValueParameter -> declaration.returnTypeRef
            is FirFunction -> declaration.receiverParameter?.typeRef
            else -> null
        }

        if (returnOrReceiverTypeRef is FirResolvedTypeRef) {
            val delegatedTypeRef = returnOrReceiverTypeRef.delegatedTypeRef
            if (delegatedTypeRef is FirUserTypeRef) {
                for (qualifierPart in delegatedTypeRef.qualifier) {
                    checkUnderscoreDiagnostics(qualifierPart.source, context, reporter, isExpression = true)

                    for (typeArgument in qualifierPart.typeArgumentList.typeArguments) {
                        checkUnderscoreDiagnostics(typeArgument.source, context, reporter, isExpression = true)
                    }
                }
            }
        }
    }
}