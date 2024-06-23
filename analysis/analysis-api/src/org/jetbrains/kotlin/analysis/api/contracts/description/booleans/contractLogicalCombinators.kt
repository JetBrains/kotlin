/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi

/**
 * See: [KaContractBooleanExpression].
 */
@KaExperimentalApi
public interface KaContractBinaryLogicExpression : KaContractBooleanExpression {
    public enum class KaLogicOperation {
        AND,
        OR,
    }

    public val left: KaContractBooleanExpression

    public val right: KaContractBooleanExpression

    public val operation: KaLogicOperation
}

@Deprecated("Use 'KaContractBinaryLogicExpression' instead.", ReplaceWith("KaContractBinaryLogicExpression"))
@KaExperimentalApi
public typealias KtContractBinaryLogicExpression = KaContractBinaryLogicExpression

/**
 * See: [KaContractBooleanExpression].
 */
@KaExperimentalApi
public interface KaContractLogicalNotExpression : KaContractBooleanExpression {
    public val argument: KaContractBooleanExpression
}

@Deprecated("Use 'KaContractLogicalNotExpression' instead.", ReplaceWith("KaContractLogicalNotExpression"))
@KaExperimentalApi
public typealias KtContractLogicalNotExpression = KaContractLogicalNotExpression
