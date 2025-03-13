/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirAbstractOverrideChecker
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.java.enhancement.EnhancedForWarningConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.FirFakeOverrideGenerator
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.utils.addToStdlib.runIf

sealed class FirOverrideJavaNullabilityWarningChecker(mppKind: MppCheckerKind) : FirAbstractOverrideChecker(mppKind) {
    object Regular : FirOverrideJavaNullabilityWarningChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    object ForExpectClass : FirOverrideJavaNullabilityWarningChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val substitutor = EnhancedForWarningConeSubstitutor(context.session.typeContext)
        val scope = declaration.unsubstitutedScope(context)
        val typeCheckerState = context.session.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = false,
            stubTypesEqualToAnything = false
        )

        declaration.symbol.processAllDeclaredCallables(context.session) { memberSymbol ->
            var anyBaseEnhanced = false
            var anyReported = false

            if (memberSymbol is FirNamedFunctionSymbol) {
                val enhancedOverrides = scope
                    .getDirectOverriddenFunctions(memberSymbol)
                    .map {
                        val substitutedBase = it.substituteOrNull(substitutor, context) ?: return@map it
                        anyBaseEnhanced = true

                        if (!anyReported && !context.session.firOverrideChecker.isOverriddenFunction(memberSymbol, substitutedBase)) {
                            anyReported = true
                            reporter.reportOn(
                                memberSymbol.source,
                                FirJvmErrors.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE,
                                memberSymbol,
                                substitutedBase,
                                context
                            )
                        }

                        substitutedBase
                    }

                if (anyBaseEnhanced && !anyReported) {
                    memberSymbol.checkReturnType(enhancedOverrides, typeCheckerState, context)?.let {
                        reporter.reportOn(
                            memberSymbol.source, FirJvmErrors.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE, memberSymbol, it, context
                        )
                    }
                }
            } else if (memberSymbol is FirPropertySymbol) {
                val enhancedOverrides = scope
                    .getDirectOverriddenProperties(memberSymbol)
                    .map {
                        val substitutedBase = it.substituteOrNull(substitutor, context) ?: return@map it
                        anyBaseEnhanced = true

                        if (!anyReported && !context.session.firOverrideChecker.isOverriddenProperty(memberSymbol, substitutedBase)) {
                            anyReported = true
                            reporter.reportOn(
                                memberSymbol.source,
                                FirJvmErrors.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE,
                                memberSymbol,
                                substitutedBase,
                                context
                            )
                        }

                        substitutedBase
                    }

                if (anyBaseEnhanced && !anyReported) {
                    memberSymbol.checkReturnType(enhancedOverrides, typeCheckerState, context)?.let {
                        reporter.reportOn(
                            memberSymbol.source, FirJvmErrors.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE, memberSymbol, it, context
                        )
                    }
                }
            }
        }
    }
}

/**
 * @see org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope.createSubstitutionOverrideFunction
 * @see org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope.createSubstitutedData
 */
private fun FirSimpleFunction.substituteOrNull(
    substitutor: EnhancedForWarningConeSubstitutor,
    context: CheckerContext,
): FirSimpleFunction? {
    symbol.lazyResolveToPhase(FirResolvePhase.TYPES)
    var isEnhanced = false

    val newParameterTypes = valueParameters.map { substitutor.substituteOrNull(it.returnTypeRef.coneType)?.also { isEnhanced = true } }
    val newContextReceiverTypes = contextParameters.map { substitutor.substituteOrNull(it.returnTypeRef.coneType)?.also { isEnhanced = true } }
    val newReturnType = substitutor.substituteOrNull(context.returnTypeCalculator.tryCalculateReturnType(this).coneType)?.also { isEnhanced = true }
    val newExtensionReceiverType =
        receiverParameter?.typeRef?.coneType?.let { substitutor.substituteOrNull(it) }?.also { isEnhanced = true }

    return runIf(isEnhanced) {
        FirFakeOverrideGenerator.createCopyForFirFunction(
            FirFakeOverrideGenerator.createSymbolForSubstitutionOverride(symbol),
            this,
            null,
            context.session,
            FirDeclarationOrigin.Enhancement,
            newDispatchReceiverType = null,
            newParameterTypes = newParameterTypes,
            newReturnType = newReturnType,
            newContextParameterTypes = newContextReceiverTypes,
            newReceiverType = newExtensionReceiverType,
            markAsOverride = true
        )
    }
}

private fun FirNamedFunctionSymbol.substituteOrNull(
    substitutor: EnhancedForWarningConeSubstitutor,
    context: CheckerContext,
): FirNamedFunctionSymbol? {
    // Ok, because `substituteOrNull` calls lazyResolveToPhase
    @OptIn(SymbolInternals::class)
    return fir.substituteOrNull(substitutor, context)?.symbol
}

/**
 * @see org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope.createSubstitutionOverrideProperty
 * @see org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope.createSubstitutedData
 */
private fun FirProperty.substituteOrNull(
    substitutor: EnhancedForWarningConeSubstitutor,
    context: CheckerContext,
): FirProperty? {
    if (!isJavaOrEnhancement) return null
    symbol.lazyResolveToPhase(FirResolvePhase.TYPES)
    var isEnhanced = false

    val newContextReceiverTypes = contextParameters.map { substitutor.substituteOrNull(it.returnTypeRef.coneType)?.also { isEnhanced = true } }
    val newReturnType = substitutor.substituteOrNull(context.returnTypeCalculator.tryCalculateReturnType(this).coneType)?.also { isEnhanced = true }
    val newExtensionReceiverType =
        receiverParameter?.typeRef?.coneType?.let { substitutor.substituteOrNull(it) }?.also { isEnhanced = true }

    return runIf(isEnhanced) {
        FirFakeOverrideGenerator.createCopyForFirProperty(
            FirFakeOverrideGenerator.createSymbolForSubstitutionOverride(symbol),
            this,
            null,
            context.session,
            FirDeclarationOrigin.Enhancement,
            newDispatchReceiverType = null,
            newReturnType = newReturnType,
            newContextParameterTypes = newContextReceiverTypes,
            newReceiverType = newExtensionReceiverType,
        )
    }
}

private fun FirPropertySymbol.substituteOrNull(
    substitutor: EnhancedForWarningConeSubstitutor,
    context: CheckerContext,
): FirPropertySymbol? {
    // Ok, because `substituteOrNull` calls `lazyResolveToPhase`
    @OptIn(SymbolInternals::class)
    return fir.substituteOrNull(substitutor, context)?.symbol
}
