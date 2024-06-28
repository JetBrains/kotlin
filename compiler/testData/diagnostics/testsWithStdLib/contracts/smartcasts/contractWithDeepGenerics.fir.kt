// OPT_IN: kotlin.contracts.ExperimentalContracts
// ISSUE: KT-43260

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed class Either<L : Any, R : Any> {
    data class Left<L : Any, R : Any>(val leftValue: L) : Either<L, R>()
    data class Right<L : Any, R : Any>(val rightValue: R) : Either<L, R>()
}

inline fun <reified L : Any, reified R : Any> Either<L, R>.isLeft(): Boolean {
    contract {
        returns(true) implies (this@isLeft is Either.Left<L, R>)
    }
    return this is Either.Left<L, R>
}

inline fun <reified L : Any, reified R : Any> Either<L, R>.isRight(): Boolean {
    contract {
        returns(true) implies (this@isRight is Either.Right<L, R>)
    }
    return this is Either.Right<L, R>
}

fun test() {
    val result: Either<Exception, Unit> = Either.Left(RuntimeException("simulating missing code"))
    if (result.isLeft()) {
        val cause = result.leftValue.cause
    }
}
