/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.signatures.KaFe10FunctionSignature
import org.jetbrains.kotlin.analysis.api.descriptors.signatures.KaFe10VariableSignature
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaAbstractSignatureSubstitutor
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.signatures.KaFunctionSignature
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.receiverType

internal class KaFe10SignatureSubstitutor(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaAbstractSignatureSubstitutor<KaFe10Session>(), KaFe10SessionComponent {
    override fun <S : KaFunctionSymbol> S.asSignature(): KaFunctionSignature<S> = withValidityAssertion {
        return KaFe10FunctionSignature(this, returnType, receiverType, valueParameters.map { it.asSignature() })
    }

    override fun <S : KaVariableSymbol> S.asSignature(): KaVariableSignature<S> = withValidityAssertion {
        return KaFe10VariableSignature(this, returnType, receiverType)
    }
}