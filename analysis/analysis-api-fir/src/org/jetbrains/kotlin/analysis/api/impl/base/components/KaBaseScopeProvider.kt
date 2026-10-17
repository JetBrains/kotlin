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
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

@KaImplementationDetail
class KaBaseScopeContext(
    scopes: List<KaScopeWithKind>,
    implicitValues: List<KaScopeImplicitValue>,
    possibleSmartCasts: List<KaSmartCastPossibility>,
    override val token: KaLifetimeToken,
) : KaScopeContext {
    private val backingImplicitValues: List<KaScopeImplicitValue> = implicitValues
    private val backingScopes: List<KaScopeWithKind> = scopes
    private val backingPossibleSmartCasts: List<KaSmartCastPossibility> = possibleSmartCasts

    override val implicitValues: List<KaScopeImplicitValue> get() = withValidityAssertion { backingImplicitValues }
    override val scopes: List<KaScopeWithKind> get() = withValidityAssertion { backingScopes }
    override val possibleSmartCasts: List<KaSmartCastPossibility> get() = withValidityAssertion { backingPossibleSmartCasts }
}

@KaImplementationDetail
class KaBaseSmartCastPossibility(
    source: KaSmartCastSource,
    smartCastTypes: List<KaType>,
    isStable: Boolean
) : KaSmartCastPossibility {
    private val backingSource: KaSmartCastSource = source
    private val backingSmartCastTypes: List<KaType> = smartCastTypes
    private val backingIsStable: Boolean = isStable

    override val token: KaLifetimeToken get() = backingSource.token

    override val source: KaSmartCastSource get() = withValidityAssertion { backingSource }
    override val smartCastTypes: List<KaType> get() = withValidityAssertion { backingSmartCastTypes }
    override val isStable: Boolean get() = withValidityAssertion { backingIsStable }
}

@KaImplementationDetail
class KaBaseSmartCastSource(
    symbol: KaDeclarationSymbol,
    dispatchReceiver: KaSmartCastSource?,
    extensionReceiver: KaSmartCastSource?,
    originalType: KaType,
) : KaSmartCastSource {
    private val backingSymbol: KaDeclarationSymbol = symbol
    private val backingDispatchReceiver: KaSmartCastSource? = dispatchReceiver
    private val backingExtensionReceiver: KaSmartCastSource? = extensionReceiver
    private val backingOriginalType: KaType = originalType

    override val token: KaLifetimeToken get() = backingSymbol.token

    override val symbol: KaDeclarationSymbol get() = withValidityAssertion { backingSymbol }
    override val dispatchReceiver: KaSmartCastSource? get() = withValidityAssertion { backingDispatchReceiver }
    override val extensionReceiver: KaSmartCastSource? get() = withValidityAssertion { backingExtensionReceiver }
    override val originalType: KaType get() = withValidityAssertion { backingOriginalType }
}

@KaImplementationDetail
class KaBaseScopeImplicitReceiverValue(
    private val backingType: KaType,
    ownerSymbol: KaSymbol,
    scopeIndexInTower: Int,
    label: String?,
) : KaImplicitReceiver {
    private val backingOwnerSymbol: KaSymbol = ownerSymbol
    private val backingScopeIndexInTower: Int = scopeIndexInTower
    private val backingLabel: String? = label
    override val token: KaLifetimeToken get() = backingType.token

    override val type: KaType get() = withValidityAssertion { backingType }
    override val ownerSymbol: KaSymbol get() = withValidityAssertion { backingOwnerSymbol }
    override val scopeIndexInTower: Int get() = withValidityAssertion { backingScopeIndexInTower }
    override val label: String? get() = withValidityAssertion { backingLabel }
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

