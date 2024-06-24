/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.resolution

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundAccess
import org.jetbrains.kotlin.analysis.api.resolution.KaCompoundVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.KaPartiallyAppliedVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol

@KaImplementationDetail
class KaBaseCompoundVariableAccessCall(
    private val backingPartiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
    compoundAccess: KaCompoundAccess,
) : KaCompoundVariableAccessCall {
    override val variablePartiallyAppliedSymbol: KaPartiallyAppliedVariableSymbol<KaVariableSymbol>
        get() = withValidityAssertion { backingPartiallyAppliedSymbol }

    override val token: KaLifetimeToken get() = backingPartiallyAppliedSymbol.token
    override val compoundAccess: KaCompoundAccess by validityAsserted(compoundAccess)
}
