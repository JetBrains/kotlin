/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

public interface KaTypeCreator {
    public fun buildClassType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit = {}): KaType

    public fun buildClassType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit = {}): KaType

    public fun buildTypeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit = {}): KaTypeParameterType
}

public interface KaTypeBuilder : KaLifetimeOwner

@Deprecated("Use 'KaTypeBuilder' instead.", replaceWith = ReplaceWith("KaTypeBuilder"))
public typealias KtTypeBuilder = KaTypeBuilder

public interface KaClassTypeBuilder : KaTypeBuilder {
    /**
     * Default value: [KaTypeNullability.NON_NULLABLE].
     */
    public var nullability: KaTypeNullability

    public val arguments: List<KaTypeProjection>

    public fun argument(argument: KaTypeProjection)

    public fun argument(type: KaType, variance: Variance = Variance.INVARIANT)
}

@Deprecated("Use 'KaClassTypeBuilder' instead.", replaceWith = ReplaceWith("KaClassTypeBuilder"))
public typealias KtClassTypeBuilder = KaClassTypeBuilder

public interface KaTypeParameterTypeBuilder : KaTypeBuilder {
    /**
     * Default value: [KaTypeNullability.NULLABLE].
     */
    public var nullability: KaTypeNullability
}

@Deprecated("Use 'KaTypeParameterTypeBuilder' instead.", replaceWith = ReplaceWith("KaTypeParameterTypeBuilder"))
public typealias KtTypeParameterTypeBuilder = KaTypeParameterTypeBuilder
