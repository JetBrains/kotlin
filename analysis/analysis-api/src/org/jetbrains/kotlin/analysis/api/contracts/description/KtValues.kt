/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol

/**
 * Represents constant reference that can be passed to `value` argument of [kotlin.contracts.ContractBuilder.returns].
 *
 * See: [org.jetbrains.kotlin.analysis.api.contracts.description.KaContractReturnsContractEffectDeclaration.KaContractReturnsSpecificValueEffectDeclaration.value]
 */
public class KaContractConstantValue(
    private val backingConstantType: KaContractConstantType,
    override val token: KaLifetimeToken,
) : KaLifetimeOwner {
    public enum class KaContractConstantType {
        NULL, TRUE, FALSE;
    }

    public val constantType: KaContractConstantType get() = withValidityAssertion { backingConstantType }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KaContractConstantValue && other.backingConstantType == backingConstantType
    }

    override fun hashCode(): Int = backingConstantType.hashCode()
}

public typealias KtContractConstantValue = KaContractConstantValue

/**
 * Represents parameter that can be passed to `value` argument of [kotlin.contracts.ContractBuilder.callsInPlace].
 */
public class KaContractParameterValue(private val backingParameterSymbol: KaParameterSymbol) : KaLifetimeOwner {
    override val token: KaLifetimeToken get() = backingParameterSymbol.token
    public val parameterSymbol: KaParameterSymbol get() = withValidityAssertion { backingParameterSymbol }

    override fun hashCode(): Int = backingParameterSymbol.hashCode()
    override fun equals(other: Any?): Boolean {
        return this === other || other is KaContractParameterValue && other.backingParameterSymbol == backingParameterSymbol
    }
}

public typealias KtContractParameterValue = KaContractParameterValue