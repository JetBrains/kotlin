// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A
interface B

fun fooB(b: B) {}

fun <T> bar(f: (T) -> Unit, e: T) {}
fun <T> baz(e: T, f: (T) -> Unit) {}

fun test(a: A, b: B) {
    // Note that diagnostic is always on callable references as they are resolved after simple arguments
    <!INAPPLICABLE_CANDIDATE!>baz<!>(a, <!UNRESOLVED_REFERENCE!>::fooB<!>)
    <!INAPPLICABLE_CANDIDATE!>bar<!>(<!UNRESOLVED_REFERENCE!>::fooB<!>, a)
}
