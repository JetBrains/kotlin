// RUN_PIPELINE_TILL: FRONTEND
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

fun Any.getOrElse2() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    is <!INVISIBLE_REFERENCE!>Left<!> -> <!UNRESOLVED_REFERENCE!>x<!>
    is <!UNRESOLVED_REFERENCE!>Right<!> -> <!UNRESOLVED_REFERENCE!>y<!>
}
