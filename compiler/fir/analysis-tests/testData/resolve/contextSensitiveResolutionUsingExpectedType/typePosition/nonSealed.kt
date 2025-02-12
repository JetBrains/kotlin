// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

interface NonSealed {
    class Left(val x: String): NonSealed
    class Right(val y: String): NonSealed
}

fun NonSealed.getOrElse() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    is <!UNRESOLVED_REFERENCE!>Left<!> -> <!UNRESOLVED_REFERENCE!>x<!>
    is <!UNRESOLVED_REFERENCE!>Right<!> -> <!UNRESOLVED_REFERENCE!>y<!>
}
