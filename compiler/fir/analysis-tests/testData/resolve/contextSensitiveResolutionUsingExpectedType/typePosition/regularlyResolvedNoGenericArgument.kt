// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface MySealed {
    class Left(val x: String): MySealed
    class Right(val y: String): MySealed
}

interface Left<T> {
    val z: String
}

fun MySealed.getOrElse() = when (this) {
    is <!NO_TYPE_ARGUMENTS_ON_RHS!>Left<!> -> <!UNRESOLVED_REFERENCE!>z<!>
    is Right -> y
    else -> ""
}
