/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkTypeRefForUnderscore
import org.jetbrains.kotlin.fir.analysis.checkers.isMalformedExpandedType
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.declarations.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.toTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

object FirAnyTypeAliasChecker : FirTypeAliasChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.isTopLevel) {
            if (declaration.isLocal) {
                reporter.reportOn(declaration.source, FirErrors.UNSUPPORTED, "Local type aliases are unsupported.", context)
            } else if (!context.languageVersionSettings.supportsFeature(LanguageFeature.NestedTypeAliases)) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.UNSUPPORTED_FEATURE,
                    LanguageFeature.NestedTypeAliases to context.languageVersionSettings,
                    context
                )
            }
        }

        val expandedTypeRef = declaration.expandedTypeRef
        val fullyExpandedType = expandedTypeRef.coneType.fullyExpandedType(context.session)

        declaration.checkTypeAliasExpansionCapturesOuterTypeParameters(fullyExpandedType, expandedTypeRef, context, reporter)

        declaration.checkTypealiasShouldExpandToClass(fullyExpandedType, expandedTypeRef, context, reporter)

        checkTypeRefForUnderscore(expandedTypeRef, context, reporter)

        val allowNullableNothing = context.languageVersionSettings.supportsFeature(LanguageFeature.NullableNothingInReifiedPosition)
        if (fullyExpandedType.isMalformedExpandedType(context, allowNullableNothing)) {
            reporter.reportOn(
                declaration.expandedTypeRef.source,
                FirErrors.TYPEALIAS_EXPANDS_TO_ARRAY_OF_NOTHINGS,
                fullyExpandedType,
                context
            )
        }
    }

    @OptIn(SymbolInternals::class)
    private fun FirTypeAlias.checkTypeAliasExpansionCapturesOuterTypeParameters(
        fullyExpandedType: ConeKotlinType,
        expandedTypeRef: FirTypeRef,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.isTopLevel || isInner || isLocal) return

        val unsubstitutedOuterTypeParameters = mutableSetOf<FirTypeParameterSymbol>()

        fun ConeKotlinType.checkRecursively() {
            for (typeArgument in typeArguments) {
                typeArgument.type?.fullyExpandedType(context.session)?.checkRecursively()
            }

            val regularClassSymbol = toRegularClassSymbol(context.session) ?: return

            val outerTypeParameterRefs = regularClassSymbol.fir.typeParameters.filterIsInstance<FirOuterClassTypeParameterRef>()
                .takeIf { it.isNotEmpty() } ?: return

            for (outerTypeParameterRef in outerTypeParameterRefs) {
                if (typeArguments.any { it.type?.toTypeParameterSymbol(context.session) == outerTypeParameterRef.symbol }) {
                    unsubstitutedOuterTypeParameters.add(outerTypeParameterRef.symbol)
                }
            }
        }

        fullyExpandedType.checkRecursively()

        if (unsubstitutedOuterTypeParameters.isNotEmpty()) {
            reporter.reportOn(
                expandedTypeRef.source,
                FirErrors.TYPEALIAS_EXPANSION_CAPTURES_OUTER_TYPE_PARAMETERS,
                unsubstitutedOuterTypeParameters,
                context
            )
        }
    }

    private fun FirTypeAlias.checkTypealiasShouldExpandToClass(
        fullyExpandedType: ConeKotlinType,
        expandedTypeRef: FirTypeRef,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        fun containsTypeParameter(type: ConeKotlinType): Boolean {
            val unwrapped = type.unwrapToSimpleTypeUsingLowerBound()

            if (unwrapped is ConeTypeParameterType) {
                return true
            }

            if (unwrapped is ConeClassLikeType && unwrapped.lookupTag.toSymbol(context.session) is FirTypeAliasSymbol) {
                for (typeArgument in unwrapped.typeArguments) {
                    val typeArgumentType = (typeArgument as? ConeKotlinType) ?: (typeArgument as? ConeKotlinTypeProjection)?.type
                    if (typeArgumentType != null && containsTypeParameter(typeArgumentType)) {
                        return true
                    }
                }
            }

            return false
        }

        if (containsTypeParameter(fullyExpandedType) || fullyExpandedType is ConeDynamicType) {
            reporter.reportOn(
                this.expandedTypeRef.source,
                FirErrors.TYPEALIAS_SHOULD_EXPAND_TO_CLASS,
                expandedTypeRef.coneType,
                context
            )
        }
    }
}
