/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaAnnotationCall
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

@KaImplementationDetail
class KaBaseAnnotationCall(
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
    argumentMapping: Map<KtExpression, KaVariableSignature<KaValueParameterSymbol>>,
) : KaAnnotationCall {
    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token

    /**
     * The function and receivers for this call.
     */
    override val partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> get() = withValidityAssertion { emptyMap() }
    override val argumentMapping: Map<KtExpression, KaVariableSignature<KaValueParameterSymbol>> by validityAsserted(argumentMapping)
}