// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

sealed interface MySealed {
    class Left(val x: String): MySealed
    class Right(val y: String): MySealed
}

fun <T : MySealed> T.getOrElse1() = when (this) {
    is Left -> x
    is Right -> y
}

fun <T> T.getOrElse2() where T : MySealed, T : CharSequence = when (this) {
    is Left -> x
    is Right -> y
}
