/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(KaAnalysisApiInternals::class)

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.KaAnalysisApiInternals
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.asKaInitializerValue
import org.jetbrains.kotlin.analysis.api.impl.base.KaContextReceiverImpl
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.create
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

internal fun FirFunctionSymbol<*>.createKtValueParameters(builder: KaSymbolByFirBuilder): List<KaValueParameterSymbol> {
    return fir.valueParameters.map { valueParameter ->
        builder.variableLikeBuilder.buildValueParameterSymbol(valueParameter.symbol)
    }
}

internal fun <D> FirBasedSymbol<D>.createKtTypeParameters(
    builder: KaSymbolByFirBuilder
): List<KaFirTypeParameterSymbol> where D : FirTypeParameterRefsOwner, D : FirDeclaration {
    return fir.typeParameters.map { typeParameter ->
        builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol)
    }
}

internal fun <D> FirBasedSymbol<D>.createRegularKtTypeParameters(
    builder: KaSymbolByFirBuilder,
): List<KaFirTypeParameterSymbol> where D : FirTypeParameterRefsOwner, D : FirDeclaration {
    return fir.typeParameters.filterIsInstance<FirTypeParameter>().map { typeParameter ->
        builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol)
    }
}

internal fun FirCallableSymbol<*>.createContextReceivers(
    builder: KaSymbolByFirBuilder
): List<KaContextReceiver> {
    return resolvedContextReceivers.map { createContextReceiver(builder, it) }
}

internal fun FirRegularClassSymbol.createContextReceivers(
    builder: KaSymbolByFirBuilder
): List<KaContextReceiver> {
    return resolvedContextReceivers.map { createContextReceiver(builder, it) }
}

private fun createContextReceiver(
    builder: KaSymbolByFirBuilder,
    contextReceiver: FirContextReceiver
) = KaContextReceiverImpl(
    builder.typeBuilder.buildKtType(contextReceiver.typeRef),
    contextReceiver.customLabelName,
    builder.token
)

internal fun FirCallableSymbol<*>.getCallableId(): CallableId? {
    return when {
        origin == FirDeclarationOrigin.DynamicScope -> null
        callableId.isLocal -> null
        else -> callableId
    }
}

internal fun FirClassLikeSymbol<*>.getClassId(): ClassId? =
    classId.takeUnless { it.isLocal }

internal fun FirCallableSymbol<*>.dispatchReceiverType(
    builder: KaSymbolByFirBuilder,
): KaType? {
    val type = if (
        origin == FirDeclarationOrigin.DynamicScope
        && (this is FirPropertySymbol || this is FirFunctionSymbol)
    ) {
        ConeDynamicType.create(builder.rootSession)
    } else {
        dispatchReceiverType
    }
    return type?.let { builder.typeBuilder.buildKtType(it) }
}

@KaExperimentalApi
internal fun FirVariableSymbol<*>.getKtConstantInitializer(builder: KaSymbolByFirBuilder): KaInitializerValue? {
    // to avoid lazy resolve
    if (fir.initializer == null) return null

    var firInitializer = resolvedInitializer ?: return null
    if (firInitializer is FirPropertyAccessExpression) {
        val calleeReference = firInitializer.calleeReference
        if (calleeReference is FirPropertyFromParameterResolvedNamedReference) {
            val valueParameterSymbol = calleeReference.resolvedSymbol as? FirValueParameterSymbol
            if (valueParameterSymbol != null) {
                firInitializer = valueParameterSymbol.resolvedDefaultValue ?: firInitializer
            }
        }
    }

    val parentIsAnnotation = dispatchReceiverType
        ?.toRegularClassSymbol(builder.rootSession)
        ?.classKind == ClassKind.ANNOTATION_CLASS

    return firInitializer.asKaInitializerValue(builder, parentIsAnnotation)
}
