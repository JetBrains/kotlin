/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KtTypeArgument
import org.jetbrains.kotlin.analysis.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.assertIsValidAndAccessible
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KtTypeParameterType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

public abstract class KtTypeCreator : KtAnalysisSessionComponent() {
    public abstract fun buildClassType(builder: KtClassTypeBuilder): KtClassType

    public abstract fun buildTypeParameterType(builder: KtTypeParameterTypeBuilder): KtTypeParameterType
}

public interface KtTypeCreatorMixIn : KtAnalysisSessionMixIn

public inline fun KtTypeCreatorMixIn.buildClassType(
    classId: ClassId,
    build: KtClassTypeBuilder.() -> Unit = {}
): KtClassType =
    analysisSession.typesCreator.buildClassType(KtClassTypeBuilder.ByClassId(classId, token).apply(build))

public inline fun KtTypeCreatorMixIn.buildClassType(
    symbol: KtClassLikeSymbol,
    build: KtClassTypeBuilder.() -> Unit = {}
): KtClassType =
    analysisSession.typesCreator.buildClassType(KtClassTypeBuilder.BySymbol(symbol, token).apply(build))

public inline fun KtTypeCreatorMixIn.buildTypeParameterType(
    symbol: KtTypeParameterSymbol,
    build: KtTypeParameterTypeBuilder.() -> Unit = {}
): KtTypeParameterType =
    analysisSession.typesCreator.buildTypeParameterType(KtTypeParameterTypeBuilder.BySymbol(symbol, token).apply(build))

public sealed class KtTypeBuilder : KtLifetimeOwner

public sealed class KtClassTypeBuilder : KtTypeBuilder() {
    private val _arguments = mutableListOf<KtTypeArgument>()

    public var nullability: KtTypeNullability = KtTypeNullability.NON_NULLABLE

    public val arguments: List<KtTypeArgument> get() = withValidityAssertion { _arguments }

    public fun argument(argument: KtTypeArgument) {
        assertIsValidAndAccessible()
        _arguments += argument
    }

    public fun argument(type: KtType, variance: Variance = Variance.INVARIANT)  {
        assertIsValidAndAccessible()
        _arguments += KtTypeArgumentWithVariance(type, variance, type.token)
    }

    public class ByClassId(public val classId: ClassId, override val token: KtLifetimeToken) : KtClassTypeBuilder()
    public class BySymbol(private val _symbol: KtClassLikeSymbol, override val token: KtLifetimeToken) : KtClassTypeBuilder() {
        public val symbol: KtClassLikeSymbol get() = withValidityAssertion { _symbol }
    }
}

public sealed class KtTypeParameterTypeBuilder : KtTypeBuilder() {
    public var nullability: KtTypeNullability = KtTypeNullability.NULLABLE

    public class BySymbol(private val _symbol: KtTypeParameterSymbol, override val token: KtLifetimeToken) : KtTypeParameterTypeBuilder() {
        public val symbol: KtTypeParameterSymbol get() = withValidityAssertion { _symbol }
    }
}

