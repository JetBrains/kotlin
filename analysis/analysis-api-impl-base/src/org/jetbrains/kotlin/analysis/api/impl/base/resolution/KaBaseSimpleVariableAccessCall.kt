/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedVariableSymbol
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccess
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType


@KaImplementationDetail
class KaBaseSimpleVariableAccessCall(
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
    typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType>,
    simpleAccess: KaSimpleVariableAccess,
) : KaSimpleVariableAccessCall {
    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token
    override val partiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol> get() = withValidityAssertion { backingPartiallyAppliedSymbol }
    override val typeArgumentsMapping: Map<KaTypeParameterSymbol, KaType> by validityAsserted(typeArgumentsMapping)
    override val simpleAccess: KaSimpleVariableAccess by validityAsserted(simpleAccess)
}