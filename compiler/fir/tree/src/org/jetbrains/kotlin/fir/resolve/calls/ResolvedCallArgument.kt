/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.calls.ResolvedCallArgument.DefaultArgument
import org.jetbrains.kotlin.fir.resolve.calls.ResolvedCallArgument.SimpleArgument
import org.jetbrains.kotlin.fir.resolve.calls.ResolvedCallArgument.VarargArgument

sealed class ResolvedCallArgument<out T> {
    abstract val arguments: List<T>

    object DefaultArgument : ResolvedCallArgument<Nothing>() {
        override val arguments: List<Nothing>
            get() = emptyList()

    }

    class SimpleArgument<T>(val callArgument: T) : ResolvedCallArgument<T>() {
        override val arguments: List<T>
            get() = listOf(callArgument)

    }

    class VarargArgument<T>(override val arguments: List<T>) : ResolvedCallArgument<T>()
}

typealias CallableReferenceMappedArguments<T> = Map<FirValueParameter, ResolvedCallArgument<T>>

fun <T, R> ResolvedCallArgument<T>.map(block: (T) -> R): ResolvedCallArgument<R> {
    return when (this) {
        is SimpleArgument -> SimpleArgument(block(callArgument))
        is VarargArgument -> VarargArgument(arguments.map(block))
        is DefaultArgument -> DefaultArgument
    }
}
