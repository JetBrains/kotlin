/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.Variance

object FirActualTypeAliasChecker : FirTypeAliasChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isActual) return

        val expandedType = declaration.expandedTypeRef.coneType.abbreviatedTypeOrSelf as? ConeClassLikeType ?: return
        val expandedTypeSymbol = expandedType.toSymbol(context.session) ?: return

        if (expandedTypeSymbol is FirTypeAliasSymbol) {
            reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_NOT_TO_CLASS, context)
        }

        for (typeParameterSymbol in expandedTypeSymbol.typeParameterSymbols) {
            if (typeParameterSymbol.variance != Variance.INVARIANT) {
                reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE, context)
                break
            }
        }

        for (typeArgument in expandedType.typeArguments) {
            if (typeArgument.kind != ProjectionKind.INVARIANT) {
                reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE, context)
                break
            }
        }

        var reportActualTypeAliasWithComplexSubstitution = false
        if (declaration.typeParameters.size != expandedType.typeArguments.size) {
            reportActualTypeAliasWithComplexSubstitution = true
        } else {
            for (i in 0 until declaration.typeParameters.size) {
                val typeArgument = expandedType.typeArguments[i]
                if (typeArgument is ConeTypeParameterType) {
                    if (declaration.typeParameters[i].symbol != typeArgument.lookupTag.typeParameterSymbol) {
                        reportActualTypeAliasWithComplexSubstitution = true
                        break
                    }
                } else if (typeArgument is ConeKotlinType && typeArgument.typeArguments.isNotEmpty()) {
                    reportActualTypeAliasWithComplexSubstitution = true
                    break
                }
            }
        }
        if (reportActualTypeAliasWithComplexSubstitution) {
            reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION, context)
        }

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) {
            // an earlier check ensures we have an ACTUAL_TYPE_ALIAS_NOT_TO_CLASS error on non-expanded type alias
            if (expandedType.isNothing) {
                reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_TO_NOTHING, context)
            }

            if (expandedType.isMarkedNullable) {
                reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_TO_NULLABLE_TYPE, context)
            }
        }
    }
}
