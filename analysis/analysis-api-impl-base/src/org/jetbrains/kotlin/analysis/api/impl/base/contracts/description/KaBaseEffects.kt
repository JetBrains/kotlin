/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.contracts.description

import com.google.common.base.Objects
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractCallsInPlaceContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConditionalContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractConstantValue
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractEffectDeclaration
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractParameterValue
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractReturnsContractEffectDeclaration.KaContractReturnsNotNullEffectDeclaration
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractReturnsContractEffectDeclaration.KaContractReturnsSpecificValueEffectDeclaration
import org.jetbrains.kotlin.analysis.api.contracts.description.KaContractReturnsContractEffectDeclaration.KaContractReturnsSuccessfullyEffectDeclaration
import org.jetbrains.kotlin.analysis.api.contracts.description.booleans.KaContractBooleanExpression
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange

class KaBaseContractCallsInPlaceContractEffectDeclaration(
    private val backingValueParameterReference: KaContractParameterValue,
    private val backingOccurrencesRange: EventOccurrencesRange,
) : KaContractCallsInPlaceContractEffectDeclaration {
    override val token: KaLifetimeToken get() = backingValueParameterReference.token

    override val valueParameterReference: KaContractParameterValue get() = withValidityAssertion { backingValueParameterReference }
    override val occurrencesRange: EventOccurrencesRange get() = withValidityAssertion { backingOccurrencesRange }

    override fun hashCode(): Int = Objects.hashCode(backingValueParameterReference, backingOccurrencesRange)

    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaBaseContractCallsInPlaceContractEffectDeclaration &&
                other.backingValueParameterReference == backingValueParameterReference &&
                other.backingOccurrencesRange == backingOccurrencesRange
    }
}

class KaBaseContractConditionalContractEffectDeclaration(
    private val backingEffect: KaContractEffectDeclaration,
    private val backingCondition: KaContractBooleanExpression,
) : KaContractConditionalContractEffectDeclaration {
    override val token: KaLifetimeToken get() = backingEffect.token

    override val effect: KaContractEffectDeclaration get() = withValidityAssertion { backingEffect }
    override val condition: KaContractBooleanExpression get() = withValidityAssertion { backingCondition }

    override fun hashCode(): Int = Objects.hashCode(backingEffect, backingCondition)
    override fun equals(other: Any?): Boolean {
        return this === other ||
                other is KaBaseContractConditionalContractEffectDeclaration &&
                other.backingEffect == backingEffect &&
                other.backingCondition == backingCondition
    }
}

object KaBaseContractReturnsContractEffectDeclarations {
    class KaBaseContractReturnsNotNullEffectDeclaration(
        override val token: KaLifetimeToken
    ) : KaContractReturnsNotNullEffectDeclaration {
        override fun equals(other: Any?): Boolean = other is KaBaseContractReturnsNotNullEffectDeclaration
        override fun hashCode(): Int = javaClass.hashCode()
    }

    class KaBaseContractReturnsSpecificValueEffectDeclaration(
        private val backingValue: KaContractConstantValue
    ) : KaContractReturnsSpecificValueEffectDeclaration {
        override val token: KaLifetimeToken get() = backingValue.token

        override val value: KaContractConstantValue get() = withValidityAssertion { backingValue }

        override fun equals(other: Any?): Boolean {
            return this === other ||
                    other is KaBaseContractReturnsSpecificValueEffectDeclaration &&
                    other.backingValue == backingValue
        }

        override fun hashCode(): Int = backingValue.hashCode()
    }

    class KaBaseContractReturnsSuccessfullyEffectDeclaration(
        override val token: KaLifetimeToken
    ) : KaContractReturnsSuccessfullyEffectDeclaration {
        override fun equals(other: Any?): Boolean = other is KaBaseContractReturnsSuccessfullyEffectDeclaration
        override fun hashCode(): Int = javaClass.hashCode()
    }
}
