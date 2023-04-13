/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.signatures

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.impl.base.types.KtChainedSubstitutor
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KtVariableLikeSignature
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneType

internal sealed class KtFirVariableLikeSignature<out S : KtVariableLikeSymbol>(ktSymbol: S) : KtVariableLikeSignature<S>(ktSymbol) {
    abstract override fun substitute(substitutor: KtSubstitutor): KtFirVariableLikeSignature<S>
}

internal class KtFirVariableLikeSubstitutorBasedSignature<out S : KtVariableLikeSymbol>(
    override val signature: KtVariableLikeSignature<S>,
    override val substitutor: KtSubstitutor,
) : KtFirVariableLikeSignature<S>(signature.symbol), SubstitutorBasedSignature<KtVariableLikeSignature<S>> {
    override val returnType: KtType by cached {
        substitutor.substitute(signature.returnType)
    }
    override val receiverType: KtType? by cached {
        signature.receiverType?.let { substitutor.substitute(it) }
    }

    override fun substitute(substitutor: KtSubstitutor): KtFirVariableLikeSignature<S> = when {
        substitutor is KtSubstitutor.Empty -> this
        else -> KtFirVariableLikeSubstitutorBasedSignature(signature, KtChainedSubstitutor(this.substitutor, substitutor))
    }
}

internal open class KtFirVariableLikeSymbolBasedSignature<out S : KtVariableLikeSymbol>(
    ktSymbol: S
) : KtFirVariableLikeSignature<S>(ktSymbol) {
    override fun substitute(substitutor: KtSubstitutor): KtFirVariableLikeSignature<S> = when {
        substitutor is KtSubstitutor.Empty -> this
        else -> KtFirVariableLikeSubstitutorBasedSignature(this, substitutor)
    }
}

internal class KtFirPropertyFirSymbolBasedSignature(
    ktSymbol: KtVariableSymbol,
    override val firSymbol: FirPropertySymbol,
    override val firSymbolBuilder: KtSymbolByFirBuilder,
) : KtFirVariableLikeSymbolBasedSignature<KtVariableSymbol>(ktSymbol), FirSymbolBasedSignature<FirPropertySymbol> {
    override val returnType: KtType by cached {
        firSymbolBuilder.typeBuilder.buildKtType(firSymbol.resolvedReturnType)
    }
    override val receiverType: KtType? by cached {
        firSymbol.resolvedReceiverTypeRef?.let { typeRef ->
            firSymbolBuilder.typeBuilder.buildKtType(typeRef.coneType)
        }
    }
}

internal class KtFirValueParameterFirSymbolBasedSignature(
    ktSymbol: KtValueParameterSymbol,
    override val firSymbol: FirValueParameterSymbol,
    override val firSymbolBuilder: KtSymbolByFirBuilder,
) : KtFirVariableLikeSymbolBasedSignature<KtValueParameterSymbol>(ktSymbol), FirSymbolBasedSignature<FirValueParameterSymbol> {
    override val returnType: KtType by cached {
        var coneType = firSymbol.resolvedReturnType
        if (firSymbol.isVararg) {
            coneType = coneType.arrayElementType() ?: coneType
        }
        firSymbolBuilder.typeBuilder.buildKtType(coneType)
    }
    override val receiverType: KtType? get() = withValidityAssertion { null }
}