// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
class A {
    <!CONFLICTING_OVERLOADS!>fun a(a: Int): Int<!> = 0

    <!CONFLICTING_OVERLOADS!>fun a(a: Int)<!> {
    }
}
