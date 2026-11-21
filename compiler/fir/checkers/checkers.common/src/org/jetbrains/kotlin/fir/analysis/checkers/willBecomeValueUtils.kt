/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.collectUpperBounds
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.typeContext
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.fir.declarations.annotationPlatformSupport
import org.jetbrains.kotlin.name.ClassId

/**
 * Class annotated specifically with `@kotlin.WillBecomeValue`.
 */
context(c: CheckerContext)
fun FirClassSymbol<*>.hasWillBecomeValueAnnotation(): Boolean =
    hasAnnotation(StandardClassIds.Annotations.WillBecomeValue, c.session)

/**
 * `kotlin.WillBecomeValue` / platform analogue if class is annotated with them.
 */
context(c: CheckerContext)
fun FirRegularClassSymbol.findWillBecomeValueLikeAnnotation(): ClassId? =
    c.session.annotationPlatformSupport.willBecomeValueAnnotations.firstOrNull {
        resolvedAnnotationsWithClassIds.hasAnnotation(it, c.session)
    }

context(_: CheckerContext)
inline fun <R> ifFutureValueType(type: ConeKotlinType, block: (FutureValueType) -> R): R? =
    type.futureValueTypeBoundOrNull()?.let(block)

context(c: CheckerContext)
fun ConeKotlinType.futureValueTypeBoundOrNull(): FutureValueType? {
    val upperBounds = collectUpperBounds(c.session.typeContext)
    return upperBounds.firstNotNullOfOrNull { type ->
        type.toRegularClassSymbol()?.findWillBecomeValueLikeAnnotation()?.let { FutureValueType(it, type) }
    }
}

class FutureValueType(
    val willBecomeValueLikeAnnotation: ClassId,
    val valueType: ConeKotlinType,
)
