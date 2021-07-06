/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgument
import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.types.KtTypeNullability
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

public abstract class KtTypeCreator : KtAnalysisSessionComponent() {
    public abstract fun buildClassType(builder: KtClassTypeBuilder): KtClassType
}

public interface KtTypeCreatorMixIn : KtAnalysisSessionMixIn


public inline fun KtTypeCreatorMixIn.buildClassType(
    classId: ClassId,
    build: KtClassTypeBuilder.() -> Unit = {}
): KtClassType =
    analysisSession.typesCreator.buildClassType(KtClassTypeBuilder.ByClassId(classId).apply(build))

public inline fun KtTypeCreatorMixIn.buildClassType(
    symbol: KtClassOrObjectSymbol,
    build: KtClassTypeBuilder.() -> Unit = {}
): KtClassType =
    analysisSession.typesCreator.buildClassType(KtClassTypeBuilder.BySymbol(symbol).apply(build))


public sealed class KtTypeBuilder

public sealed class KtClassTypeBuilder : KtTypeBuilder() {
    private val _arguments = mutableListOf<KtTypeArgument>()

    public var nullability: KtTypeNullability = KtTypeNullability.NON_NULLABLE

    public val arguments: List<KtTypeArgument> get() = _arguments

    public fun argument(argument: KtTypeArgument) {
        _arguments += argument
    }

    public fun argument(type: KtType, variance: Variance = Variance.INVARIANT) {
        _arguments += KtTypeArgumentWithVariance(type, variance, type.token)
    }

    public class ByClassId(public val classId: ClassId) : KtClassTypeBuilder()
    public class BySymbol(public val symbol: KtClassOrObjectSymbol) : KtClassTypeBuilder()
}

