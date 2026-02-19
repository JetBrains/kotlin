/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkTypeRefForUnderscore
import org.jetbrains.kotlin.fir.analysis.checkers.isMalformedExpandedType
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.checkers.requireFeatureSupport
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.annotationPlatformSupport
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toTypeAliasSymbol
import org.jetbrains.kotlin.fir.resolve.toTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

object FirAnyTypeAliasChecker : FirTypeAliasChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirTypeAlias) {
        if (!context.isTopLevel) {
            declaration.requireFeatureSupport(if (declaration.isLocal) LanguageFeature.LocalTypeAliases else LanguageFeature.NestedTypeAliases)
        }

        val expandedTypeRef = declaration.expandedTypeRef
        val fullyExpandedType = expandedTypeRef.coneType.fullyExpandedType()

        declaration.checkTypeAliasExpansionCapturesOuterTypeParameters(fullyExpandedType, expandedTypeRef)

        if (!fullyExpandedType.hasError()) {
            val expandedClassSymbol = fullyExpandedType.toRegularClassSymbol()

            if (expandedClassSymbol?.classKind == ClassKind.ANNOTATION_CLASS &&
                context.session.annotationPlatformSupport.requiredAnnotations.contains(expandedClassSymbol.classId)
            ) {
                reporter.reportOn(expandedTypeRef.source, FirErrors.TYPEALIAS_EXPANDS_TO_COMPILER_REQUIRED_ANNOTATION, expandedClassSymbol)
            }

            if (expandedClassSymbol == null || fullyExpandedType is ConeDynamicType) {
                // Skip reporting if RHS is a type alias to keep the amount of error minimal
                if (expandedTypeRef.coneType.toTypeAliasSymbol() == null) {
                    reporter.reportOn(expandedTypeRef.source, FirErrors.TYPEALIAS_SHOULD_EXPAND_TO_CLASS, fullyExpandedType)
                }
            }
        }

        checkTypeRefForUnderscore(expandedTypeRef)

        val allowNullableNothing = LanguageFeature.NullableNothingInReifiedPosition.isEnabled()
        if (fullyExpandedType.isMalformedExpandedType(allowNullableNothing)) {
            reporter.reportOn(
                declaration.expandedTypeRef.source,
                FirErrors.TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS,
                fullyExpandedType
            )
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    @OptIn(SymbolInternals::class)
    private fun FirTypeAlias.checkTypeAliasExpansionCapturesOuterTypeParameters(
        fullyExpandedType: ConeKotlinType,
        expandedTypeRef: FirTypeRef,
    ) {
        if (context.isTopLevel || isInner) return

        val unsubstitutedOuterTypeParameters = mutableSetOf<FirTypeParameterSymbol>()

        fun checkRecursively(coneType: ConeKotlinType) {
            for (typeArgument in coneType.typeArguments) {
                typeArgument.type?.fullyExpandedType()?.let { checkRecursively(it) }
            }

            val typeParameterSymbol = coneType.toTypeParameterSymbol() ?: return

            if (symbol != typeParameterSymbol.containingDeclarationSymbol) {
                unsubstitutedOuterTypeParameters.add(typeParameterSymbol)
            }
        }

        checkRecursively(fullyExpandedType)

        if (unsubstitutedOuterTypeParameters.isNotEmpty()) {
            reporter.reportOn(
                expandedTypeRef.source,
                FirErrors.TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS,
                unsubstitutedOuterTypeParameters
            )
        }
    }
}
