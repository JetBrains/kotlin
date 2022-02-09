// FIR_IDENTICAL
// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER

fun takeArray(array: Array<String>) {}

fun test() {
    "foo bar".<!UNRESOLVED_REFERENCE!>split<!>(<!UNSUPPORTED!>[""]<!>)
    <!UNRESOLVED_REFERENCE!>unresolved<!>(<!UNSUPPORTED!>[""]<!>)
    takeArray(<!UNSUPPORTED!>[""]<!>)
    val v = <!UNSUPPORTED!>[""]<!>
    <!UNSUPPORTED!>[""]<!>
    <!UNSUPPORTED!>[1, 2, 3]<!>.size
}

fun baz(arg: Array<Int> = <!UNSUPPORTED!>[]<!>) {
    if (true) <!UNSUPPORTED!>["yes"]<!> else {<!UNSUPPORTED!>["no"]<!>}
}

class Foo(
    val v: Array<Int> = <!UNSUPPORTED!>[]<!>
)
