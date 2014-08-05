// !DIAGNOSTICS: -UNUSED_PARAMETER

data class <!CONFLICTING_JVM_DECLARATIONS!>A(val x: Int, val y: String)<!> {
    <!CONFLICTING_OVERLOADS!>fun copy(x: Int, y: String)<!> = x
    <!CONFLICTING_OVERLOADS!>fun copy(x: Int, y: String)<!> = A(x, y)
}