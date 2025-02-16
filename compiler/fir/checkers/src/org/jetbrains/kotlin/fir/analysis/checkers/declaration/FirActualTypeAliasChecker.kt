/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.collectAllFunctions
import org.jetbrains.kotlin.fir.scopes.getDeclaredConstructors
import org.jetbrains.kotlin.fir.scopes.getSingleClassifier
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.mpp.ActualTypealiasToSpecialAnnotationUtils.isAnnotationProhibitedInActualTypeAlias
import org.jetbrains.kotlin.types.Variance

object FirActualTypeAliasChecker : FirTypeAliasChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.isActual) return

        declaration.checkDefaultArgumentsInExpectWithActualTypeAlias(context, reporter)

        val expandedType = declaration.expandedTypeRef.coneType.abbreviatedTypeOrSelf as? ConeClassLikeType ?: return
        val expandedTypeSymbol = expandedType.toSymbol(context.session) ?: return

        if (expandedTypeSymbol is FirTypeAliasSymbol) {
            reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_NOT_TO_CLASS, context)
        }

        declaration.checkTypeAliasToClassWithDeclarationSiteVariance(expandedTypeSymbol, context, reporter)
        declaration.checkTypeAliasWithUseSiteVariance(expandedType, context, reporter)
        declaration.checkTypeAliasWithComplexSubstitution(expandedType, context, reporter)

        if (context.languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions)) {
            // an earlier check ensures we have an ACTUAL_TYPE_ALIAS_NOT_TO_CLASS error on non-expanded type alias
            if (expandedType.isNothing) {
                reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_TO_NOTHING, context)
            }

            if (expandedType.isMarkedNullable) {
                reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPE_ALIAS_TO_NULLABLE_TYPE, context)
            }

            if (expandedTypeSymbol.classKind == ClassKind.ANNOTATION_CLASS) {
                val classId = expandedTypeSymbol.classId
                if (isAnnotationProhibitedInActualTypeAlias(classId)) {
                    reporter.reportOn(declaration.source, FirErrors.ACTUAL_TYPEALIAS_TO_SPECIAL_ANNOTATION, classId, context)
                }
            }
        }
    }

    private fun FirTypeAlias.checkDefaultArgumentsInExpectWithActualTypeAlias(context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.let {
                !it.supportsFeature(LanguageFeature.MultiplatformRestrictions) || !it.supportsFeature(LanguageFeature.MultiplatformRestrictions)
            }
        ) {
            return
        }

        val actualTypealiasSymbol = symbol
        // We want to report errors even if a candidate is incompatible, but it's single
        val expectedSingleCandidate = actualTypealiasSymbol.getSingleMatchedExpectForActualOrNull() ?: return
        val expectClassSymbol = expectedSingleCandidate as FirRegularClassSymbol

        val membersWithDefaultValueParameters = getMembersWithDefaultValueParametersUnlessAnnotation(expectClassSymbol)
        if (membersWithDefaultValueParameters.isEmpty()) return

        reporter.reportOn(
            source,
            FirErrors.DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS,
            expectClassSymbol,
            membersWithDefaultValueParameters,
            context
        )
    }

    private fun getMembersWithDefaultValueParametersUnlessAnnotation(classSymbol: FirClassSymbol<*>): List<FirFunctionSymbol<*>> {
        val result = mutableListOf<FirFunctionSymbol<*>>()

        fun collectFunctions(classSymbol: FirClassSymbol<*>) {
            if (classSymbol.classKind == ClassKind.ANNOTATION_CLASS) {
                return
            }
            val memberScope = classSymbol.declaredMemberScope(classSymbol.moduleData.session, memberRequiredPhase = null)
            val functionsAndConstructors = memberScope
                .run { collectAllFunctions() + getDeclaredConstructors() }

            functionsAndConstructors.filterTo(result) { it.valueParameterSymbols.any(FirValueParameterSymbol::hasDefaultValue) }

            val nestedClasses = memberScope.getClassifierNames()
                .mapNotNull { memberScope.getSingleClassifier(it) as? FirClassSymbol<*> }

            for (nestedClassSymbol in nestedClasses) {
                collectFunctions(nestedClassSymbol)
            }
        }

        collectFunctions(classSymbol)
        return result
    }

    private fun FirTypeAlias.checkTypeAliasToClassWithDeclarationSiteVariance(
        expandedTypeSymbol: FirClassLikeSymbol<*>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        for (typeParameterSymbol in expandedTypeSymbol.typeParameterSymbols) {
            if (typeParameterSymbol.variance != Variance.INVARIANT) {
                reporter.reportOn(source, FirErrors.ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE, context)
                break
            }
        }
    }

    private fun FirTypeAlias.checkTypeAliasWithUseSiteVariance(
        expandedType: ConeClassLikeType,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        for (typeArgument in expandedType.typeArguments) {
            if (typeArgument.kind != ProjectionKind.INVARIANT) {
                reporter.reportOn(source, FirErrors.ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE, context)
                break
            }
        }
    }

    private fun FirTypeAlias.checkTypeAliasWithComplexSubstitution(
        expandedType: ConeClassLikeType,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        var reportActualTypeAliasWithComplexSubstitution = false
        if (typeParameters.size != expandedType.typeArguments.size) {
            reportActualTypeAliasWithComplexSubstitution = true
        } else {
            for (i in 0 until typeParameters.size) {
                val typeArgument = expandedType.typeArguments[i]
                if (typeArgument is ConeTypeParameterType) {
                    if (typeParameters[i].symbol != typeArgument.lookupTag.typeParameterSymbol) {
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
            reporter.reportOn(source, FirErrors.ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION, context)
        }
    }
}
