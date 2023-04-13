/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.signatures

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirFunctionSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.types.KtChainedSubstitutor
import org.jetbrains.kotlin.analysis.api.signatures.KtFunctionLikeSignature
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol

internal sealed class KtFirFunctionLikeSignature<out S : KtFunctionLikeSymbol>(ktSymbol: S) : KtFunctionLikeSignature<S>(ktSymbol) {
    abstract override fun substitute(substitutor: KtSubstitutor): KtFirFunctionLikeSignature<S>
}

internal class KtFirFunctionLikeSubstitutorBasedSignature<out S : KtFunctionLikeSymbol>(
    override val signature: KtFunctionLikeSignature<S>,
    override val substitutor: KtSubstitutor,
) : KtFirFunctionLikeSignature<S>(signature.symbol), SubstitutorBasedSignature<KtFunctionLikeSignature<S>> {
    override val returnType: KtType by cached {
        substitutor.substitute(signature.returnType)
    }
    override val receiverType: KtType? by cached {
        signature.receiverType?.let { substitutor.substitute(it) }
    }
    override val valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>> by cached {
        signature.valueParameters.map { it.substitute(substitutor) }
    }

    override fun substitute(substitutor: KtSubstitutor): KtFirFunctionLikeSignature<S> = when {
        substitutor is KtSubstitutor.Empty -> this
        else -> KtFirFunctionLikeSubstitutorBasedSignature(signature, KtChainedSubstitutor(this.substitutor, substitutor))
    }
}

internal open class KtFirFunctionLikeSymbolBasedSignature<S : KtFunctionLikeSymbol>(ktSymbol: S) : KtFirFunctionLikeSignature<S>(ktSymbol) {
    override val valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>> by cached {
        ktSymbol.valueParameters.map { KtFirVariableLikeSymbolBasedSignature(it) }
    }

    override fun substitute(substitutor: KtSubstitutor): KtFirFunctionLikeSignature<S> = when {
        substitutor is KtSubstitutor.Empty -> this
        else -> KtFirFunctionLikeSubstitutorBasedSignature(this, substitutor)
    }
}

internal class KtFirFunctionFirSymbolBasedSignature(
    ktSymbol: KtFirFunctionSymbol,
    override val firSymbol: FirNamedFunctionSymbol,
    override val firSymbolBuilder: KtSymbolByFirBuilder,
) : KtFirFunctionLikeSymbolBasedSignature<KtFirFunctionSymbol>(ktSymbol), FirSymbolBasedSignature<FirNamedFunctionSymbol> {
    override val returnType: KtType by cached {
        firSymbolBuilder.typeBuilder.buildKtType(firSymbol.resolvedReturnType)
    }
    override val receiverType: KtType? by cached {
        firSymbol.resolvedReceiverTypeRef?.let { firSymbolBuilder.typeBuilder.buildKtType(it) }
    }
    override val valueParameters: List<KtVariableLikeSignature<KtValueParameterSymbol>> by cached {
        ktSymbol.valueParameters.zip(firSymbol.fir.valueParameters).map { (ktValueParameterSymbol, firValueParameter) ->
            KtFirValueParameterFirSymbolBasedSignature(ktValueParameterSymbol, firValueParameter.symbol, firSymbolBuilder)
        }
    }
}