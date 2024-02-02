/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.declarations.BirAnnotationContainer
import org.jetbrains.kotlin.bir.expressions.BirConstructorCall
import org.jetbrains.kotlin.bir.symbols.BirTypeAliasSymbol
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.TypeArgumentMarker

/**
 * An argument for a generic parameter. Can be either [BirTypeProjection], or [BirStarProjection].
 */
sealed interface BirTypeArgument : TypeArgumentMarker {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

data object BirStarProjection : BirTypeArgument

interface BirTypeProjection : BirTypeArgument {
    val variance: Variance
    val type: BirType
}

class BirTypeAbbreviation(
    val typeAlias: BirTypeAliasSymbol,
    val hasQuestionMark: Boolean,
    val arguments: List<BirTypeArgument>,
    override val annotations: List<BirConstructorCall>
) : BirAnnotationContainer