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
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol
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
    source: KaSmartCastSource<KaVariableSymbol>,
    resultingTypes: List<KaType>,
    isStable: Boolean
) : KaSmartCastPossibility {
    private val backingSource: KaSmartCastSource<KaVariableSymbol> = source
    private val backingResultingTypes: List<KaType> = resultingTypes
    private val backingIsStable: Boolean = isStable

    override val token: KaLifetimeToken get() = backingSource.token

    override val source: KaSmartCastSource<KaVariableSymbol> get() = withValidityAssertion { backingSource }
    override val resultingTypes: List<KaType> get() = withValidityAssertion { backingResultingTypes }
    override val isStable: Boolean get() = withValidityAssertion { backingIsStable }
}

@KaImplementationDetail
class KaBaseSmartCastSource<T : KaDeclarationSymbol>(
    symbol: T,
    dispatchReceiver: KaSmartCastSource<KaDeclarationSymbol>?,
    extensionReceiver: KaSmartCastSource<KaDeclarationSymbol>?,
    type: KaType,
) : KaSmartCastSource<T> {
    private val backingSymbol: T = symbol
    private val backingDispatchReceiver: KaSmartCastSource<KaDeclarationSymbol>? = dispatchReceiver
    private val backingExtensionReceiver: KaSmartCastSource<KaDeclarationSymbol>? = extensionReceiver
    private val backingType: KaType = type

    override val token: KaLifetimeToken get() = backingSymbol.token

    override val symbol: T get() = withValidityAssertion { backingSymbol }
    override val dispatchReceiver: KaSmartCastSource<KaDeclarationSymbol>? get() = withValidityAssertion { backingDispatchReceiver }
    override val extensionReceiver: KaSmartCastSource<KaDeclarationSymbol>? get() = withValidityAssertion { backingExtensionReceiver }
    override val type: KaType get() = withValidityAssertion { backingType }
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

