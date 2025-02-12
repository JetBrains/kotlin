// RUN_PIPELINE_TILL: FRONTEND
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

fun MySealed.getOrElse1() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    is <!OVERLOAD_RESOLUTION_AMBIGUITY!>Left<!> -> <!UNRESOLVED_REFERENCE!>x<!>
    is Right -> y
}

fun Any.getOrElse2() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    is <!OVERLOAD_RESOLUTION_AMBIGUITY!>Left<!> -> <!UNRESOLVED_REFERENCE!>x<!>
    is <!UNRESOLVED_REFERENCE!>Right<!> -> <!UNRESOLVED_REFERENCE!>y<!>
}
