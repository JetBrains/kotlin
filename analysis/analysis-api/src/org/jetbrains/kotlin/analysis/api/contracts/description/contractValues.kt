/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol

/**
 * Represents constant reference that can be passed to `value` argument of [kotlin.contracts.ContractBuilder.returns].
 *
 * See: [org.jetbrains.kotlin.analysis.api.contracts.description.KaContractReturnsContractEffectDeclaration.KaContractReturnsSpecificValueEffectDeclaration.value]
 */
@KaExperimentalApi
public interface KaContractConstantValue : KaLifetimeOwner {
    @KaExperimentalApi
    public enum class KaContractConstantType {
        NULL,
        TRUE,
        FALSE,
    }

    public val constantType: KaContractConstantType
}

/**
 * Represents parameter that can be passed to `value` argument of [kotlin.contracts.ContractBuilder.callsInPlace].
 */
@KaExperimentalApi
public sealed interface KaContractParameterValue : KaLifetimeOwner {
    /**
     * A symbol to which this parameter points.
     */
    public val symbol: KaSymbol
}

/**
 * Represents an explicit parameter which is declared on the corresponding contract owner.
 * Examples: all [KaParameterSymbol] hierarchy.
 */
@KaExperimentalApi
public interface KaContractExplicitParameterValue : KaContractParameterValue {
    public override val symbol: KaParameterSymbol
}

/**
 * Represents an owner of the corresponding contract owner.
 * Example:
 * ```kotlin
 * sealed class Result {
 *     class Success : Result()
 *     class Failure : Result()
 *
 *     fun isFailure(): Boolean {
 *         contract {
 *             returns() implies (this is Failure)
 *         }
 *
 *         return this is Failure
 *     }
 * }
 * ```
 *
 * `this` is represented by this value.
 */
@KaExperimentalApi
public interface KaContractOwnerParameterValue : KaContractParameterValue {
    public override val symbol: KaClassSymbol
}
