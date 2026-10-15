/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.requireFeatureSupport
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.isEquals
import org.jetbrains.kotlin.fir.declarations.utils.equalityBoundType
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeAmbiguityError
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.getContainingClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.constructStarProjectedType
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.StandardClassIds

object FirExplicitEqualityBoundAnnotationChecker : FirValueParameterChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirValueParameter) {
        val annotation =
            declaration.annotations.getAnnotationByClassId(StandardClassIds.Annotations.EqualityBound, context.session) ?: return
        if ((declaration.containingDeclarationSymbol as? FirNamedFunctionSymbol)?.isEquals(context.session) != true) {
            reporter.reportOn(
                annotation.source,
                FirErrors.UNSUPPORTED,
                "'EqualityBound' annotation is only supported for parameters of 'equals' operator."
            )
        } else {
            annotation.requireFeatureSupport(LanguageFeature.StrictEquals) {
                val typeFromTypesPhase = declaration.equalityBoundType?.fullyExpandedType()
                val argumentOfGetClass = annotation
                    .findArgumentByName(StandardClassIds.Annotations.ParameterNames.equalityBound)
                    ?.let { it as? FirGetClassCall }
                    ?.argument
                    ?: return
                val typeFromAnnotationArgumentsPhase = argumentOfGetClass
                    .resolvedType
                    .takeIf { argumentOfGetClass is FirResolvedQualifier }
                    ?.fullyExpandedType()
                    ?: return

                // Sometimes type resolve and body resolve can produce different results for
                // identically looking qualifiers / type refs. The following two conditions are effectively safeguards
                // for these cases. Note that we only report them if body resolve was successful enough,
                // otherwise we'll report some other error anyway.
                if (typeFromTypesPhase == null || typeFromTypesPhase is ConeErrorType) {
                    (typeFromTypesPhase?.diagnostic as? ConeAmbiguityError)?.candidates?.mapNotNull {
                        (it.symbol as? FirClassLikeSymbol)?.constructStarProjectedType()?.fullyExpandedType()
                    }?.let { possibleTypes ->
                        reporter.reportOn(
                            argumentOfGetClass.source,
                            FirErrors.AMBIGUOUSLY_RESOLVED_EQUALITY_BOUND_ARGUMENT,
                            possibleTypes,
                        )
                        return
                    }
                    reporter.reportOn(argumentOfGetClass.source, FirErrors.UNRESOLVED_EQUALITY_BOUND_ARGUMENT)
                    return
                }

                if (typeFromTypesPhase != typeFromAnnotationArgumentsPhase) {
                    reporter.reportOn(
                        argumentOfGetClass.source,
                        FirErrors.AMBIGUOUSLY_RESOLVED_EQUALITY_BOUND_ARGUMENT,
                        [typeFromTypesPhase, typeFromAnnotationArgumentsPhase],
                    )
                    return
                }

                if (typeFromTypesPhase.typeArguments.any { it !is ConeStarProjection }) {
                    assert((argumentOfGetClass as? FirResolvedQualifier)?.qualifierSymbol is FirTypeAliasSymbol) {
                        "Non-star type arguments can only come from type aliases."
                    }
                    reporter.reportOn(
                        argumentOfGetClass.source,
                        FirErrors.EQUALITY_BOUND_ARGUMENT_EXPANDS_TO_NON_STAR_PROJECTED,
                        typeFromTypesPhase,
                    )
                    return
                }

                val starProjectedTypeForEnclosingClass =
                    declaration.containingDeclarationSymbol.getContainingClassSymbol()?.constructStarProjectedType() ?: return

                if (!starProjectedTypeForEnclosingClass.isSubtypeOf(typeFromTypesPhase, session = context.session)) {
                    reporter.reportOn(
                        argumentOfGetClass.source,
                        FirErrors.EQUALITY_BOUND_NOT_SUPERTYPE_OF_CONTAINING_CLASS,
                        typeFromTypesPhase,
                        starProjectedTypeForEnclosingClass,
                    )
                }
            }
        }
    }
}
