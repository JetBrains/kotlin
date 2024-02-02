/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types.utils

import org.jetbrains.kotlin.bir.declarations.BirTypeParameter
import org.jetbrains.kotlin.bir.symbols.BirTypeParameterSymbol
import org.jetbrains.kotlin.bir.types.*
import org.jetbrains.kotlin.bir.util.deepCopy
import org.jetbrains.kotlin.utils.memoryOptimizedMap


fun BirType.substitute(params: List<BirTypeParameter>, arguments: List<BirType>): BirType =
    substitute((params.map { it.symbol } zip arguments).toMap())

fun BirType.substitute(substitutionMap: Map<BirTypeParameterSymbol, BirType>): BirType {
    if (this !is BirSimpleType || substitutionMap.isEmpty()) return this

    val newAnnotations = annotations.memoryOptimizedMap { it.deepCopy() }

    substitutionMap[classifier]?.let { substitutedType ->
        // Add nullability and annotations from original type
        return substitutedType
            .mergeNullability(this)
            .addAnnotations(newAnnotations)
    }

    val newArguments = arguments.memoryOptimizedMap {
        when (it) {
            is BirTypeProjection -> makeTypeProjection(it.type.substitute(substitutionMap), it.variance)
            is BirStarProjection -> it
        }
    }

    return BirSimpleTypeImpl(
        classifier,
        nullability,
        newArguments,
        newAnnotations
    )
}