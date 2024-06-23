/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol

/**
 * Represents constant reference that can be passed to `value` argument of [kotlin.contracts.ContractBuilder.returns].
 *
 * See: [org.jetbrains.kotlin.analysis.api.contracts.description.KaContractReturnsContractEffectDeclaration.KaContractReturnsSpecificValueEffectDeclaration.value]
 */
@KaExperimentalApi
public interface KaContractConstantValue : KaLifetimeOwner {
    public enum class KaContractConstantType {
        NULL,
        TRUE,
        FALSE,
    }

    public val constantType: KaContractConstantType
}

@Deprecated("Use 'KaContractConstantValue' instead", ReplaceWith("KaContractConstantValue"))
@KaExperimentalApi
public typealias KtContractConstantValue = KaContractConstantValue

/**
 * Represents parameter that can be passed to `value` argument of [kotlin.contracts.ContractBuilder.callsInPlace].
 */
@KaExperimentalApi
public interface KaContractParameterValue : KaLifetimeOwner {
    public val parameterSymbol: KaParameterSymbol
}

@Deprecated("Use 'KaContractParameterValue' instead", ReplaceWith("KaContractParameterValue"))
@KaExperimentalApi
public typealias KtContractParameterValue = KaContractParameterValue
