/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaDelegatedConstructorCall
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.signatures.KaVariableSignature
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

@KaImplementationDetail
class KaBaseDelegatedConstructorCall(
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol>,
    kind: KaDelegatedConstructorCall.Kind,
    argumentMapping: LinkedHashMap<KtExpression, KaVariableSignature<KaValueParameterSymbol>>,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
) : KaDelegatedConstructorCall {
    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token
    override val partiallyAppliedSymbol: KaPartiallyAppliedFunctionSymbol<KaConstructorSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }
    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> by validityAsserted(typeArgumentsMapping)
    override val kind: KaDelegatedConstructorCall.Kind by validityAsserted(kind)
    override val argumentMapping: LinkedHashMap<KtExpression, KaVariableSignature<KaValueParameterSymbol>>
            by validityAsserted(argumentMapping)
}