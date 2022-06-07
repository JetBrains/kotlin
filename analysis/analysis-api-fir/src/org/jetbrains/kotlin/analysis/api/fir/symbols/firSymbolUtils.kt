/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.KtInitializerValue
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.asKtInitializerValue
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getElementTextInContext
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.ensureResolved
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtDeclaration


internal fun FirCallableSymbol<*>.invalidModalityError(): Nothing {
    error(
        """|Symbol modality should not be null, looks like the FIR symbol was not properly resolved
                   |
                   |${fir.renderWithType(FirRenderer.RenderMode.WithResolvePhases)}
                   |
                   |${(fir.psi as? KtDeclaration)?.getElementTextInContext()}""".trimMargin()
    )
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


internal fun FirCallableSymbol<*>.getCallableIdIfNonLocal(): CallableId? =
    callableId.takeUnless { it.isLocal }

internal fun FirClassLikeSymbol<*>.getClassIdIfNonLocal(): ClassId? =
    classId.takeUnless { it.isLocal }

internal fun FirCallableSymbol<*>.dispatchReceiverType(
    builder: KtSymbolByFirBuilder,
): KtType? {
    return dispatchReceiverType?.let { builder.typeBuilder.buildKtType(it) }
}

internal fun FirVariableSymbol<*>.getKtConstantInitializer(): KtInitializerValue? {
    ensureResolved(FirResolvePhase.BODY_RESOLVE)
    val firInitializer = fir.initializer ?: return null
    return firInitializer.asKtInitializerValue()
}