/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.contracts.description.booleans

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail

/**
 * See: [KaContractBooleanExpression].
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaContractBinaryLogicExpression : KaContractBooleanExpression {
    @KaExperimentalApi
    public enum class KaLogicOperation {
        AND,
        OR,
    }

    public val left: KaContractBooleanExpression

    public val right: KaContractBooleanExpression

    public val operation: KaLogicOperation
}

/**
 * See: [KaContractBooleanExpression].
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaContractLogicalNotExpression : KaContractBooleanExpression {
    public val argument: KaContractBooleanExpression
}
