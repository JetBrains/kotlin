/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.isResolved
import org.jetbrains.kotlin.name.Name

abstract class ConeCallAtom {
    companion object;

    abstract val fir: FirElement
    abstract val expression: FirExpression
}

class ConeResolvedAtom(override val fir: FirExpression) : ConeCallAtom() {
    init {
        check(fir.isResolved) { "ConeResolvedAtom should be created only for resolved expressions" }
    }

    override val expression: FirExpression
        get() = fir
}

class ConeSafeCallAtom(override val fir: FirSafeCallExpression, val selector: ConeCallAtom?) : ConeCallAtom() {
    override val expression: FirSafeCallExpression
        get() = fir
}

class ConeErrorExpressionAtom(override val fir: FirErrorExpression, val subAtom: ConeCallAtom?) : ConeCallAtom() {
    override val expression: FirErrorExpression
        get() = fir
}

class ConeBlockAtom(override val fir: FirBlock, val lastExpressionAtom: ConeCallAtom?) : ConeCallAtom() {
    override val expression: FirBlock
        get() = fir
}

sealed class ConeWrappedExpressionAtom(val subAtom: ConeCallAtom) : ConeCallAtom() {
    abstract override val fir: FirWrappedArgumentExpression
    abstract override val expression: FirWrappedArgumentExpression

    val isSpread: Boolean
        get() = fir.isSpread
}

class ConeSpreadExpressionAtom(
    override val fir: FirSpreadArgumentExpression,
    subAtom: ConeCallAtom
) : ConeWrappedExpressionAtom(subAtom) {
    override val expression: FirSpreadArgumentExpression
        get() = fir
}

class ConeNamedArgumentAtom(
    override val fir: FirNamedArgumentExpression,
    subAtom: ConeCallAtom
) : ConeWrappedExpressionAtom(subAtom) {
    override val expression: FirNamedArgumentExpression
        get() = fir

    val name: Name
        get() = fir.name
}

fun ConeCallAtom.unwrap(): ConeCallAtom {
    return when (this) {
        is ConeWrappedExpressionAtom -> subAtom
        else -> this
    }
}
