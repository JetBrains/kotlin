// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
class A {
    <!CONFLICTING_OVERLOADS!>fun a(a: Int): Int<!> = 0

    <!CONFLICTING_OVERLOADS!>fun a(a: Int)<!> {
    }
}
