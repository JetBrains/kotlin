/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaImplicitReceiver
import org.jetbrains.kotlin.analysis.api.components.KaScopeContext
import org.jetbrains.kotlin.analysis.api.components.KaScopeImplicitArgumentValue
import org.jetbrains.kotlin.analysis.api.components.KaScopeImplicitValue
import org.jetbrains.kotlin.analysis.api.components.KaScopeWithKind
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

@KaImplementationDetail
class KaBaseScopeContext(
    scopes: List<KaScopeWithKind>,
    implicitValues: List<KaScopeImplicitValue>,
    override val token: KaLifetimeToken,
) : KaScopeContext {
    override val implicitValues: List<KaScopeImplicitValue> by validityAsserted(implicitValues)
    override val scopes: List<KaScopeWithKind> by validityAsserted(scopes)
}

@KaImplementationDetail
class KaBaseScopeImplicitReceiverValue(
    private val backingType: KaType,
    ownerSymbol: KaSymbol,
    scopeIndexInTower: Int,
) : KaImplicitReceiver {
    override val token: KaLifetimeToken get() = backingType.token

    override val type: KaType get() = withValidityAssertion { backingType }
    override val ownerSymbol: KaSymbol by validityAsserted(ownerSymbol)
    override val scopeIndexInTower: Int by validityAsserted(scopeIndexInTower)
}

@KaImplementationDetail
class KaBaseScopeImplicitArgumentValue(
    private val backingType: KaType,
    symbol: KaContextParameterSymbol,
    scopeIndexInTower: Int,
) : KaScopeImplicitArgumentValue {
    override val token: KaLifetimeToken get() = backingType.token

    override val type: KaType get() = withValidityAssertion { backingType }
    override val scopeIndexInTower: Int by validityAsserted(scopeIndexInTower)
    override val symbol: KaContextParameterSymbol by validityAsserted(symbol)
}

