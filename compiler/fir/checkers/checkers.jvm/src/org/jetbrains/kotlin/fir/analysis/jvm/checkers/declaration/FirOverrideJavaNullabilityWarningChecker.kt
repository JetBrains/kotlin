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
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (declaration.isExpect) return
            super.check(declaration)
        }
    }

    object ForExpectClass : FirOverrideJavaNullabilityWarningChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirClass) {
            if (!declaration.isExpect) return
            super.check(declaration)
        }
    }

    @OptIn(ScopeFunctionRequiresPrewarm::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val substitutor = EnhancedForWarningConeSubstitutor(context.session.typeContext)
        val scope = declaration.unsubstitutedScope()
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
                        val substitutedBase = it.substituteOrNull(substitutor) ?: return@map it
                        anyBaseEnhanced = true

                        if (!anyReported && !context.session.firOverrideChecker.isOverriddenFunction(memberSymbol, substitutedBase)) {
                            anyReported = true
                            reporter.reportOn(
                                memberSymbol.source,
                                FirJvmErrors.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE,
                                memberSymbol,
                                substitutedBase
                            )
                        }

                        substitutedBase
                    }

                if (anyBaseEnhanced && !anyReported) {
                    memberSymbol.checkReturnType(enhancedOverrides, typeCheckerState)?.let {
                        reporter.reportOn(
                            memberSymbol.source, FirJvmErrors.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE, memberSymbol, it
                        )
                    }
                }
            } else if (memberSymbol is FirPropertySymbol) {
                val enhancedOverrides = scope
                    .getDirectOverriddenProperties(memberSymbol)
                    .map {
                        val substitutedBase = it.substituteOrNull(substitutor) ?: return@map it
                        anyBaseEnhanced = true

                        if (!anyReported && !context.session.firOverrideChecker.isOverriddenProperty(memberSymbol, substitutedBase)) {
                            anyReported = true
                            reporter.reportOn(
                                memberSymbol.source,
                                FirJvmErrors.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE,
                                memberSymbol,
                                substitutedBase
                            )
                        }

                        substitutedBase
                    }

                if (anyBaseEnhanced && !anyReported) {
                    memberSymbol.checkReturnType(enhancedOverrides, typeCheckerState)?.let {
                        reporter.reportOn(
                            memberSymbol.source, FirJvmErrors.WRONG_NULLABILITY_FOR_JAVA_OVERRIDE, memberSymbol, it
                        )
                    }
                }
            }
        }
    }
}

context(context: CheckerContext)
/**
 * @see org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope.createSubstitutionOverrideFunction
 * @see org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope.createSubstitutedData
 */
private fun FirNamedFunction.substituteOrNull(
    substitutor: EnhancedForWarningConeSubstitutor,
): FirNamedFunction? {
    symbol.lazyResolveToPhase(FirResolvePhase.TYPES)
    var isEnhanced = false

    val newParameterTypes = valueParameters.map { substitutor.substituteOrNull(it.returnTypeRef.coneType)?.also { isEnhanced = true } }
    val newContextReceiverTypes =
        contextParameters.map { substitutor.substituteOrNull(it.returnTypeRef.coneType)?.also { isEnhanced = true } }
    val newReturnType =
        substitutor.substituteOrNull(context.returnTypeCalculator.tryCalculateReturnType(this).coneType)?.also { isEnhanced = true }
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

context(context: CheckerContext)
private fun FirNamedFunctionSymbol.substituteOrNull(
    substitutor: EnhancedForWarningConeSubstitutor,
): FirNamedFunctionSymbol? {
    // Ok, because `substituteOrNull` calls lazyResolveToPhase
    @OptIn(SymbolInternals::class)
    return fir.substituteOrNull(substitutor)?.symbol
}

context(context: CheckerContext)
/**
 * @see org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope.createSubstitutionOverrideProperty
 * @see org.jetbrains.kotlin.fir.scopes.impl.FirClassSubstitutionScope.createSubstitutedData
 */
private fun FirProperty.substituteOrNull(
    substitutor: EnhancedForWarningConeSubstitutor,
): FirProperty? {
    if (!isJavaOrEnhancement) return null
    symbol.lazyResolveToPhase(FirResolvePhase.TYPES)
    var isEnhanced = false

    val newContextReceiverTypes =
        contextParameters.map { substitutor.substituteOrNull(it.returnTypeRef.coneType)?.also { isEnhanced = true } }
    val newReturnType =
        substitutor.substituteOrNull(context.returnTypeCalculator.tryCalculateReturnType(this).coneType)?.also { isEnhanced = true }
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

context(context: CheckerContext)
private fun FirPropertySymbol.substituteOrNull(
    substitutor: EnhancedForWarningConeSubstitutor,
): FirPropertySymbol? {
    // Ok, because `substituteOrNull` calls `lazyResolveToPhase`
    @OptIn(SymbolInternals::class)
    return fir.substituteOrNull(substitutor)?.symbol
}
