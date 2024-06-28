/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.components.KaImplicitReceiver
import org.jetbrains.kotlin.analysis.api.components.KaScopeContext
import org.jetbrains.kotlin.analysis.api.components.KaScopeWithKind
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

class KaBaseScopeContext(
    scopes: List<KaScopeWithKind>,
    implicitReceivers: List<KaImplicitReceiver>,
    override val token: KaLifetimeToken
) : KaScopeContext {
    override val implicitReceivers: List<KaImplicitReceiver> by validityAsserted(implicitReceivers)

    override val scopes: List<KaScopeWithKind> by validityAsserted(scopes)
}

class KaBaseImplicitReceiver(
    private val backingType: KaType,
    ownerSymbol: KaSymbol,
    scopeIndexInTower: Int
) : KaImplicitReceiver {
    override val token: KaLifetimeToken get() = backingType.token

    override val type: KaType get() = withValidityAssertion { backingType }
    override val ownerSymbol: KaSymbol by validityAsserted(ownerSymbol)
    override val scopeIndexInTower: Int by validityAsserted(scopeIndexInTower)
}

