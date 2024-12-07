/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.components

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaClassTypeBuilder
import org.jetbrains.kotlin.analysis.api.components.KaTypeCreator
import org.jetbrains.kotlin.analysis.api.components.KaTypeParameterTypeBuilder
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseStarTypeProjection
import org.jetbrains.kotlin.analysis.api.impl.base.types.KaBaseTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

@KaImplementationDetail
abstract class KaBaseTypeCreator<T : KaSession> : KaSessionComponent<T>(), KaTypeCreator {
    override fun buildStarTypeProjection(): KaStarTypeProjection = KaBaseStarTypeProjection(token)
}

@KaImplementationDetail
sealed class KaBaseClassTypeBuilder : KaClassTypeBuilder {
    private val backingArguments = mutableListOf<KaTypeProjection>()

    override var nullability: KaTypeNullability = KaTypeNullability.NON_NULLABLE
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion { field = value }
        }

    override val arguments: List<KaTypeProjection> get() = withValidityAssertion { backingArguments }

    override fun argument(argument: KaTypeProjection): Unit = withValidityAssertion {
        backingArguments += argument
    }

    override fun argument(type: KaType, variance: Variance): Unit = withValidityAssertion {
        backingArguments += KaBaseTypeArgumentWithVariance(type, variance, type.token)
    }

    class ByClassId(classId: ClassId, override val token: KaLifetimeToken) : KaBaseClassTypeBuilder() {
        val classId: ClassId by validityAsserted(classId)
    }

    class BySymbol(symbol: KaClassLikeSymbol, override val token: KaLifetimeToken) : KaBaseClassTypeBuilder() {
        val symbol: KaClassLikeSymbol by validityAsserted(symbol)
    }
}

@KaImplementationDetail
sealed class KaBaseTypeParameterTypeBuilder : KaTypeParameterTypeBuilder {
    override var nullability: KaTypeNullability = KaTypeNullability.NULLABLE
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion { field = value }
        }

    class BySymbol(symbol: KaTypeParameterSymbol, override val token: KaLifetimeToken) : KaBaseTypeParameterTypeBuilder() {
        val symbol: KaTypeParameterSymbol by validityAsserted(symbol)
    }
}
