// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun returnsLambdaWithCallsInPlace(block: () -> Unit): () -> Unit {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    return block
}

fun returnsLambdaWithCallsInPlaceAtMostOnce(block: () -> Unit): () -> Unit {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return block
}

fun callsAndReturnsLambda(block: () -> Unit): (() -> Unit)? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    return block
}

fun returnsNotCallsInPlaceLambda(block: () -> Unit): (() -> Unit)? {
    return block
}

fun callsLambdaWithoutReturn(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun conditionalReturnWithCallsInPlace(block: () -> Unit, condition: Boolean, block3 : () -> Unit): () -> Unit {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return if (condition) block else block3
}

fun multipleParametersOneReturned(block1: () -> Unit, block2: () -> Unit): () -> Unit {
    contract {
        callsInPlace(block1, InvocationKind.UNKNOWN)
        callsInPlace(block2, InvocationKind.EXACTLY_ONCE)
    }
    block2()
    return block1
}

fun <T> callCallsInPlaceInReturnExpression(condition: Boolean, action: () -> T): T {
    contract {
        callsInPlace(action, InvocationKind.EXACTLY_ONCE)
    }
    return if (condition) {
        action()
    } else {
        action()
    }
}

@OptIn(ExperimentalContracts::class)
fun <T> synchronizedLike(lock: Any, block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return synchronizedImpl(lock, block) <!UNCHECKED_CAST!>as T<!>
}

@OptIn(ExperimentalContracts::class)
fun synchronizedImpl(lock: Any, block: () -> Any?): Any? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

class Either<out T, out E>(val value: T, val errorOrNull: E)

inline fun <R, T, E> Either<T, E>.fold(
    onValue: (value: T) -> R,
    onError: (error: E) -> R,
): R {
    contract {
        callsInPlace(onValue, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onError, InvocationKind.AT_MOST_ONCE)
    }
    return when (val error = errorOrNull) {
        null -> onValue(value)
        else -> onError(error)
    }
}

inline fun <R> runSafely(action: () -> R, onFailure: (Throwable) -> Unit): R? {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }
    return try {
        action()
    } catch (e: Throwable) {
        onFailure(e)
        null
    }
}

/* GENERATED_FIR_TAGS: contractCallsEffect, contracts, functionDeclaration, functionalType, ifExpression, lambdaLiteral,
nullableType */
