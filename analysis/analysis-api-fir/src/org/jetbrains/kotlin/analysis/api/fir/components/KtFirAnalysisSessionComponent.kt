/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.analysis.api.KtStarProjectionTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.diagnostics.KtDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.diagnostics.KT_DIAGNOSTIC_CONVERTER
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirGenericSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirMapBackedSubstitutor
import org.jetbrains.kotlin.analysis.api.fir.types.KtFirType
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnostic
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.toFirDiagnostics
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutorByMap
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.model.convertVariance
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal interface KtFirAnalysisSessionComponent {
    val analysisSession: KtFirAnalysisSession

    val project: Project get() = analysisSession.project
    val rootModuleSession: FirSession get() = analysisSession.firResolveSession.useSiteFirSession
    val typeContext: ConeInferenceContext get() = rootModuleSession.typeContext
    val firSymbolBuilder get() = analysisSession.firSymbolBuilder
    val firResolveSession get() = analysisSession.firResolveSession

    fun ConeKotlinType.asKtType() = analysisSession.firSymbolBuilder.typeBuilder.buildKtType(this)

    fun KtPsiDiagnostic.asKtDiagnostic(): KtDiagnosticWithPsi<*> =
        KT_DIAGNOSTIC_CONVERTER.convert(analysisSession, this as KtDiagnostic)

    fun ConeDiagnostic.asKtDiagnostic(
        source: KtSourceElement,
        qualifiedAccessSource: KtSourceElement?,
    ): KtDiagnosticWithPsi<*>? {
        val firDiagnostic = toFirDiagnostics(analysisSession.useSiteSession, source, qualifiedAccessSource).firstOrNull() ?: return null
        check(firDiagnostic is KtPsiDiagnostic)
        return firDiagnostic.asKtDiagnostic()
    }

    val KtType.coneType: ConeKotlinType
        get() {
            require(this is KtFirType)
            return coneType
        }

    val KtTypeArgument.coneTypeProjection: ConeTypeProjection
        get() = when (this) {
            is KtStarProjectionTypeArgument -> ConeStarProjection
            is KtTypeArgumentWithVariance -> {
                typeContext.createTypeArgument(type.coneType, variance.convertVariance()) as ConeTypeProjection
            }
        }

    fun createTypeCheckerContext(): TypeCheckerState {
        // TODO use correct session here,
        return analysisSession.firResolveSession.useSiteFirSession.typeContext.newTypeCheckerState(
            errorTypesEqualToAnything = true,
            stubTypesEqualToAnything = true
        )
    }

    fun FirQualifiedAccess.createSubstitutorFromTypeArguments(): KtSubstitutor? {
        return createConeSubstitutorFromTypeArguments()?.toKtSubstitutor()
    }

    fun FirQualifiedAccess.createSubstitutorFromTypeArguments(callableSymbol: FirCallableSymbol<*>): KtSubstitutor {
        return createConeSubstitutorFromTypeArguments(callableSymbol).toKtSubstitutor()
    }

    fun FirQualifiedAccess.createConeSubstitutorFromTypeArguments(): ConeSubstitutor? {
        val symbol = when (val calleeReference = calleeReference) {
            is FirResolvedNamedReference -> calleeReference.resolvedSymbol as? FirCallableSymbol<*>
            is FirErrorNamedReference -> calleeReference.candidateSymbol as? FirCallableSymbol<*>
            else -> null
        } ?: return null
        return createConeSubstitutorFromTypeArguments(symbol)
    }

    fun FirQualifiedAccess.createConeSubstitutorFromTypeArguments(callableSymbol: FirCallableSymbol<*>): ConeSubstitutor {
        val typeArgumentMap = mutableMapOf<FirTypeParameterSymbol, ConeKotlinType>()
        for (i in typeArguments.indices) {
            val type = typeArguments[i].safeAs<FirTypeProjectionWithVariance>()?.typeRef?.coneType
            if (type != null) {
                typeArgumentMap[callableSymbol.typeParameterSymbols[i]] = type
            }
        }
        val coneSubstitutor = substitutorByMap(typeArgumentMap, rootModuleSession)
        return coneSubstitutor
    }

    fun ConeSubstitutor.toKtSubstitutor(): KtSubstitutor {
        return when (this) {
            ConeSubstitutor.Empty -> KtSubstitutor.Empty(analysisSession.token)
            is ConeSubstitutorByMap -> KtFirMapBackedSubstitutor(this, analysisSession.firSymbolBuilder, analysisSession.token)
            else -> KtFirGenericSubstitutor(this, analysisSession.firSymbolBuilder, analysisSession.token)
        }
    }
}
