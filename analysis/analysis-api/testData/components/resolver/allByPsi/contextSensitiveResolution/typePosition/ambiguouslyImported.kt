// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: other.kt
package a
class Left

// FILE: other2.kt
package b
class Left

// FILE: main.kt
import a.*
import b.*

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
