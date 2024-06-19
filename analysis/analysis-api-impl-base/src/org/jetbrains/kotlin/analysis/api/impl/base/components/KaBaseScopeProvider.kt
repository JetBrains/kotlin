/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.components.KaImplicitReceiver
import org.jetbrains.kotlin.analysis.api.components.KaScopeContext
import org.jetbrains.kotlin.analysis.api.components.KaScopeKind
import org.jetbrains.kotlin.analysis.api.components.KaScopeWithKind
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import java.util.Objects

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

object KaBaseScopeKinds {
    class LocalScope(override val indexInTower: Int) : KaScopeKind.LocalScope

    class TypeScope(override val indexInTower: Int) : KaScopeKind.TypeScope

    class TypeParameterScope(override val indexInTower: Int) : KaScopeKind.TypeParameterScope

    class PackageMemberScope(override val indexInTower: Int) : KaScopeKind.PackageMemberScope

    class ExplicitSimpleImportingScope(override val indexInTower: Int) : KaScopeKind.ExplicitSimpleImportingScope

    class ExplicitStarImportingScope(override val indexInTower: Int) : KaScopeKind.ExplicitStarImportingScope

    class DefaultSimpleImportingScope(override val indexInTower: Int) : KaScopeKind.DefaultSimpleImportingScope

    class DefaultStarImportingScope(override val indexInTower: Int) : KaScopeKind.DefaultStarImportingScope

    class StaticMemberScope(override val indexInTower: Int) : KaScopeKind.StaticMemberScope

    class ScriptMemberScope(override val indexInTower: Int) : KaScopeKind.ScriptMemberScope
}

class KaBaseScopeWithKind(
    private val backingScope: KaScope,
    private val backingKind: KaScopeKind,
    override val token: KaLifetimeToken,
) : KaScopeWithKind {
    override val scope: KaScope get() = withValidityAssertion { backingScope }
    override val kind: KaScopeKind get() = withValidityAssertion { backingKind }

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaBaseScopeWithKind &&
                other.backingScope == backingScope &&
                other.backingKind == backingKind
    }

    override fun hashCode(): Int = Objects.hash(backingScope, backingKind)
}
