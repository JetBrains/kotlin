/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
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
    private val backingImplicitValues: List<KaScopeImplicitValue> = implicitValues
    private val backingScopes: List<KaScopeWithKind> = scopes

    override val implicitValues: List<KaScopeImplicitValue> get() = withValidityAssertion { backingImplicitValues }
    override val scopes: List<KaScopeWithKind> get() = withValidityAssertion { backingScopes }
}

@KaImplementationDetail
class KaBaseScopeImplicitReceiverValue(
    private val backingType: KaType,
    ownerSymbol: KaSymbol,
    scopeIndexInTower: Int,
) : KaImplicitReceiver {
    private val backingOwnerSymbol: KaSymbol = ownerSymbol
    private val backingScopeIndexInTower: Int = scopeIndexInTower
    override val token: KaLifetimeToken get() = backingType.token

    override val type: KaType get() = withValidityAssertion { backingType }
    override val ownerSymbol: KaSymbol get() = withValidityAssertion { backingOwnerSymbol }
    override val scopeIndexInTower: Int get() = withValidityAssertion { backingScopeIndexInTower }
}

@KaImplementationDetail
class KaBaseScopeImplicitArgumentValue(
    private val backingType: KaType,
    symbol: KaContextParameterSymbol,
    scopeIndexInTower: Int,
) : KaScopeImplicitArgumentValue {
    override val token: KaLifetimeToken get() = backingType.token
    private val backingSymbol: KaContextParameterSymbol = symbol
    private val backingScopeIndexInTower: Int = scopeIndexInTower

    override val type: KaType get() = withValidityAssertion { backingType }
    override val scopeIndexInTower: Int get() = withValidityAssertion { backingScopeIndexInTower }
    override val symbol: KaContextParameterSymbol get() = withValidityAssertion { backingSymbol }
}

