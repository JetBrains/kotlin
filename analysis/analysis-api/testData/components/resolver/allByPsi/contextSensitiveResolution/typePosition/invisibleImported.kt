// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: other.kt
package a
private class Left

// FILE: main.kt
import a.*

sealed interface MySealed {
    class Left(val x: String): MySealed
    class Right(val y: String): MySealed
}

fun MySealed.getOrElse1() = when (this) {
    is Left -> x
    is Right -> y
}

fun Any.getOrElse2() = when (this) {
    is Left -> x
    is Right -> y
}
