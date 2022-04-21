/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.symbols

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.findPsi
import org.jetbrains.kotlin.analysis.api.fir.symbols.pointers.KtFirSamConstructorSymbolPointer
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.symbols.KtSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.pointers.KtSymbolPointer
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirModuleResolveState
import org.jetbrains.kotlin.fir.resolve.getHasStableParameterNames
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.isExtension
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

internal class KtFirSamConstructorSymbol(
    override val firSymbol: FirNamedFunctionSymbol,
    override val resolveState: LLFirModuleResolveState,
    override val token: ValidityToken,
    private val builder: KtSymbolByFirBuilder
) : KtSamConstructorSymbol(), KtFirSymbol<FirNamedFunctionSymbol> {
    override val psi: PsiElement? by cached { firSymbol.findPsi() }
    override val name: Name get() = withValidityAssertion { firSymbol.name }

    override val returnType: KtType get() = withValidityAssertion { firSymbol.returnType(builder) }

    override val valueParameters: List<KtValueParameterSymbol>
        get() = withValidityAssertion {
            firSymbol.createKtValueParameters(builder)
        }

    override val hasStableParameterNames: Boolean
        get() = withValidityAssertion {
            firSymbol.fir.getHasStableParameterNames(firSymbol.moduleData.session)
        }

    override val isExtension: Boolean get() = withValidityAssertion { firSymbol.isExtension }

    override val receiverType: KtType? get() = withValidityAssertion { firSymbol.receiverType(builder) }

    override val callableIdIfNonLocal: CallableId? get() = withValidityAssertion { firSymbol.getCallableIdIfNonLocal() }

    override fun createPointer(): KtSymbolPointer<KtSamConstructorSymbol> {
        val callableId = firSymbol.callableId
        return KtFirSamConstructorSymbolPointer(ClassId(callableId.packageName, callableId.callableName))
    }
}
