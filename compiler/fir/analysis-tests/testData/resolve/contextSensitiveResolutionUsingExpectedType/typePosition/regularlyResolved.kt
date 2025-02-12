// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface MySealed {
    class Left(val x: String): MySealed
    class Right(val y: String): MySealed
}

interface Left {
    val z: String
}

fun MySealed.getOrElse() = when (this) {
    is Left -> z
    is Right -> y
    else -> ""
}
