/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.contracts.description

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConstantValue
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConstantValue.KaContractConstantType
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractParameterValue
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol

@KaImplementationDetail
class KaBaseContractConstantValue(
    private val backingConstantType: KaContractConstantType,
    override val token: KaLifetimeToken,
) : KaContractConstantValue {
    override val constantType: KaContractConstantType get() = withValidityAssertion { backingConstantType }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KaBaseContractConstantValue && other.backingConstantType == backingConstantType
    }

    override fun hashCode(): Int = backingConstantType.hashCode()
}

@KaImplementationDetail
class KaBaseContractParameterValue(private val backingParameterSymbol: KaParameterSymbol) : KaContractParameterValue {
    override val token: KaLifetimeToken get() = backingParameterSymbol.token

    override val parameterSymbol: KaParameterSymbol get() = withValidityAssertion { backingParameterSymbol }

    override fun equals(other: Any?): Boolean {
        return this === other || other is KaBaseContractParameterValue && other.backingParameterSymbol == backingParameterSymbol
    }

    override fun hashCode(): Int = backingParameterSymbol.hashCode()
}
