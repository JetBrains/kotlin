// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

interface NonSealed {
    class Left(val x: String): NonSealed
    class Right(val y: String): NonSealed
}

fun NonSealed.getOrElse() = when (this) {
    is Left -> x
    is Right -> y
}
