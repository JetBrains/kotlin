/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

public abstract class KaTypeCreator : KaSessionComponent() {
    public abstract fun buildClassType(builder: KaClassTypeBuilder): KaType

    public abstract fun buildTypeParameterType(builder: KaTypeParameterTypeBuilder): KaTypeParameterType
}

public typealias KtTypeCreator = KaTypeCreator

public interface KaTypeCreatorMixIn : KaSessionMixIn

public typealias KtTypeCreatorMixIn = KaTypeCreatorMixIn

public inline fun KaTypeCreatorMixIn.buildClassType(
    classId: ClassId,
    build: KaClassTypeBuilder.() -> Unit = {},
): KaType =
    analysisSession.typesCreator.buildClassType(KaClassTypeBuilder.ByClassId(classId, token).apply(build))

public inline fun KaTypeCreatorMixIn.buildClassType(
    symbol: KaClassLikeSymbol,
    build: KaClassTypeBuilder.() -> Unit = {},
): KaType =
    analysisSession.typesCreator.buildClassType(KaClassTypeBuilder.BySymbol(symbol, token).apply(build))

public inline fun KaTypeCreatorMixIn.buildTypeParameterType(
    symbol: KaTypeParameterSymbol,
    build: KaTypeParameterTypeBuilder.() -> Unit = {},
): KaTypeParameterType =
    analysisSession.typesCreator.buildTypeParameterType(KaTypeParameterTypeBuilder.BySymbol(symbol, token).apply(build))

public sealed class KaTypeBuilder : KaLifetimeOwner

public typealias KtTypeBuilder = KaTypeBuilder

public sealed class KaClassTypeBuilder : KaTypeBuilder() {
    private val backingArguments = mutableListOf<KaTypeProjection>()

    public var nullability: KaTypeNullability = KaTypeNullability.NON_NULLABLE
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion { field = value }
        }

    public val arguments: List<KaTypeProjection> get() = withValidityAssertion { backingArguments }

    public fun argument(argument: KaTypeProjection): Unit = withValidityAssertion {
        backingArguments += argument
    }

    public fun argument(type: KaType, variance: Variance = Variance.INVARIANT): Unit = withValidityAssertion {
        backingArguments += KaTypeArgumentWithVariance(type, variance, type.token)
    }

    public class ByClassId(classId: ClassId, override val token: KaLifetimeToken) : KaClassTypeBuilder() {
        public val classId: ClassId by validityAsserted(classId)
    }

    public class BySymbol(symbol: KaClassLikeSymbol, override val token: KaLifetimeToken) : KaClassTypeBuilder() {
        public val symbol: KaClassLikeSymbol by validityAsserted(symbol)
    }
}

public typealias KtClassTypeBuilder = KaClassTypeBuilder

public sealed class KaTypeParameterTypeBuilder : KaTypeBuilder() {
    public var nullability: KaTypeNullability = KaTypeNullability.NULLABLE
        get() = withValidityAssertion { field }
        set(value) {
            withValidityAssertion { field = value }
        }

    public class BySymbol(symbol: KaTypeParameterSymbol, override val token: KaLifetimeToken) : KaTypeParameterTypeBuilder() {
        public val symbol: KaTypeParameterSymbol by validityAsserted(symbol)
    }
}

public typealias KtTypeParameterTypeBuilder = KaTypeParameterTypeBuilder