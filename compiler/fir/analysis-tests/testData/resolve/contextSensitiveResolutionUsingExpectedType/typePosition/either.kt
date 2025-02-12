// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface Either<out E, out A> {
    data class  Left<out E>(val error: E): Either<E, Nothing>
    data class Right<out A>(val value: A): Either<Nothing, A>
}

fun <E, A> Either<E, A>.getOrElse(default: A) = when (this) {
    is Left -> default
    is Right -> value
}
