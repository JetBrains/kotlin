/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractParameterValue
import org.jetbrains.kotlin.analysis.api.types.KaType

/**
 * See: [KaContractBooleanExpression].
 */
public interface KaContractIsInstancePredicateExpression : KaContractBooleanExpression {
    public val argument: KaContractParameterValue

    public val type: KaType

    public val isNegated: Boolean

    public fun negated(): KaContractIsInstancePredicateExpression
}

@Deprecated("Use 'KaContractIsInstancePredicateExpression' instead.", ReplaceWith("KaContractIsInstancePredicateExpression"))
public typealias KtContractIsInstancePredicateExpression = KaContractIsInstancePredicateExpression

/**
 * See: [KaContractBooleanExpression].
 */
public interface KaContractIsNullPredicateExpression : KaContractBooleanExpression {
    public val argument: KaContractParameterValue

    public val isNegated: Boolean

    public fun negated(): KaContractIsNullPredicateExpression
}

@Deprecated("Use 'KaContractIsNullPredicateExpression' instead.", ReplaceWith("KaContractIsNullPredicateExpression"))
public typealias KtContractIsNullPredicateExpression = KaContractIsNullPredicateExpression
