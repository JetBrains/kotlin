/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KtAnalysisApiInternals::class)

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KtInitializerValue
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.asKtInitializerValue
import org.jetbrains.kotlin.analysis.api.impl.base.KtContextReceiverImpl
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment


internal fun FirCallableSymbol<*>.invalidModalityError(): Nothing {
    errorWithAttachment("Symbol modality should not be null, looks like the FIR symbol was not properly resolved") {
        withFirEntry("fir", this@invalidModalityError.fir)
    }
}

internal fun FirFunctionSymbol<*>.createKtValueParameters(builder: KtSymbolByFirBuilder): List<KtValueParameterSymbol> {
    return fir.valueParameters.map { valueParameter ->
        builder.variableLikeBuilder.buildValueParameterSymbol(valueParameter.symbol)
    }
}

internal fun <D> FirBasedSymbol<D>.createKtTypeParameters(
    builder: KtSymbolByFirBuilder
): List<KtFirTypeParameterSymbol> where D : FirTypeParameterRefsOwner, D : FirDeclaration {
    return fir.typeParameters.map { typeParameter ->
        builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol)
    }
}

internal fun <D> FirBasedSymbol<D>.createRegularKtTypeParameters(
    builder: KtSymbolByFirBuilder,
): List<KtFirTypeParameterSymbol> where D : FirTypeParameterRefsOwner, D : FirDeclaration {
    return fir.typeParameters.filterIsInstance<FirTypeParameter>().map { typeParameter ->
        builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol)
    }
}

internal fun FirCallableSymbol<*>.createContextReceivers(
    builder: KtSymbolByFirBuilder
): List<KtContextReceiver> {
    return resolvedContextReceivers.map { createContextReceiver(builder, it) }
}

internal fun FirRegularClassSymbol.createContextReceivers(
    builder: KtSymbolByFirBuilder
): List<KtContextReceiver> {
    return resolvedContextReceivers.map { createContextReceiver(builder, it) }
}

private fun createContextReceiver(
    builder: KtSymbolByFirBuilder,
    contextReceiver: FirContextReceiver
) = KtContextReceiverImpl(
    builder.typeBuilder.buildKtType(contextReceiver.typeRef),
    contextReceiver.customLabelName,
    builder.token
)

internal fun FirCallableSymbol<*>.getCallableIdIfNonLocal(): CallableId? =
    callableId.takeUnless { it.isLocal }

internal fun FirClassLikeSymbol<*>.getClassIdIfNonLocal(): ClassId? =
    classId.takeUnless { it.isLocal }

internal fun FirCallableSymbol<*>.dispatchReceiverType(
    builder: KtSymbolByFirBuilder,
): KtType? {
    return dispatchReceiverType?.let { builder.typeBuilder.buildKtType(it) }
}

internal fun FirVariableSymbol<*>.getKtConstantInitializer(resolveSession: LLFirResolveSession): KtInitializerValue? {
    // to avoid lazy resolve
    if (fir.initializer == null) return null

    lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
    var firInitializer = fir.initializer ?: return null
    if (firInitializer is FirPropertyAccessExpression) {
        val calleeReference = firInitializer.calleeReference
        if (calleeReference is FirPropertyFromParameterResolvedNamedReference) {
            val valueParameterSymbol = calleeReference.resolvedSymbol as? FirValueParameterSymbol
            if (valueParameterSymbol != null) {
                valueParameterSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                firInitializer = valueParameterSymbol.fir.defaultValue ?: firInitializer
            }
        }
    }
    val parentIsAnnotation = dispatchReceiverType
        ?.toRegularClassSymbol(resolveSession.useSiteFirSession)
        ?.classKind == ClassKind.ANNOTATION_CLASS
    return firInitializer.asKtInitializerValue(moduleData.session, parentIsAnnotation)
}
