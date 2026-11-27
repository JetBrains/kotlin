/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.KaInitializerValue
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirContextReceiver
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.components.firSymbolBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.asKaInitializerValue
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.impl.FirPropertyFromParameterResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.create
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId

context(analysisSession: KaFirSession)
internal fun FirFunctionSymbol<*>.createKtValueParameters(): List<KaValueParameterSymbol> {
    return fir.valueParameters.map { valueParameter ->
        firSymbolBuilder.variableBuilder.buildValueParameterSymbol(valueParameter.symbol)
    }
}

context(analysisSession: KaFirSession)
internal fun FirCallableSymbol<*>.createKaContextParameters(): List<KaContextParameterSymbol> {
    return fir.contextParameters.map { contextParameter ->
        firSymbolBuilder.variableBuilder.buildContextParameterSymbol(contextParameter.symbol)
    }
}

context(analysisSession: KaFirSession)
internal fun <D> FirBasedSymbol<D>.createKtTypeParameters(
): List<KaTypeParameterSymbol> where D : FirTypeParameterRefsOwner, D : FirDeclaration {
    return fir.typeParameters.map { typeParameter ->
        firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol)
    }
}

context(analysisSession: KaFirSession)
internal fun <D> FirBasedSymbol<D>.createRegularKtTypeParameters(
): List<KaTypeParameterSymbol> where D : FirTypeParameterRefsOwner, D : FirDeclaration {
    return fir.typeParameters.filterIsInstance<FirTypeParameter>().map { typeParameter ->
        firSymbolBuilder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol)
    }
}

context(analysisSession: KaFirSession)
internal fun FirCallableSymbol<*>.createContextReceivers(
): List<KaContextReceiver> {
    return contextParameterSymbols.map { createContextReceiver(it) }
}

context(analysisSession: KaFirSession)
internal fun FirRegularClassSymbol.createContextReceivers(
): List<KaContextReceiver> {
    return resolvedContextParameters.map { createContextReceiver(it.symbol) }
}

context(analysisSession: KaFirSession)
private fun createContextReceiver(
    contextParameter: FirValueParameterSymbol
) = KaFirContextReceiver(
    firSymbolBuilder.typeBuilder.buildKtType(contextParameter.resolvedReturnType),
    contextParameter.name,
    analysisSession.token
)

internal fun FirCallableSymbol<*>.getCallableId(): CallableId? {
    return when {
        origin == FirDeclarationOrigin.DynamicScope -> null
        isLocal -> null
        else -> callableId
    }
}

internal fun FirClassLikeSymbol<*>.getClassId(): ClassId? =
    classId.takeUnless { isLocal }

context(analysisSession: KaFirSession)
internal fun FirCallableSymbol<*>.dispatchReceiverType(): KaType? {
    val type = if (
        origin == FirDeclarationOrigin.DynamicScope
        && (this is FirPropertySymbol || this is FirFunctionSymbol)
    ) {
        ConeDynamicType.create(analysisSession.firSession)
    } else {
        dispatchReceiverType
    }
    return type?.let { firSymbolBuilder.typeBuilder.buildKtType(it) }
}

context(analysisSession: KaFirSession)
internal fun FirVariableSymbol<*>.getKtConstantInitializer(): KaInitializerValue? {
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
        ?.toRegularClassSymbol(analysisSession.firSession)
        ?.classKind == ClassKind.ANNOTATION_CLASS

    return firInitializer.asKaInitializerValue(parentIsAnnotation)
}

internal val FirBasedSymbol<*>.kaSymbolModality: KaSymbolModality
    get() = when (this) {
        is FirCallableSymbol<*> -> modality
        is FirClassLikeSymbol<*> -> modality
        else -> Modality.FINAL
    }.asKaSymbolModality
