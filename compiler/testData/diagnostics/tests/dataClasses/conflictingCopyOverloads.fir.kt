// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER

data class <!CONFLICTING_OVERLOADS!>A(val x: Int, val y: String)<!> {
    <!CONFLICTING_OVERLOADS!>fun copy(x: Int, y: String)<!> = x
    <!CONFLICTING_OVERLOADS!>fun copy(x: Int, y: String)<!> = A(x, y)
}