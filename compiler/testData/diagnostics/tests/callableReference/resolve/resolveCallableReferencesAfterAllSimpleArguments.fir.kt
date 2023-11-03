// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A
interface B

fun fooB(b: B) {}

fun <T> bar(f: (T) -> Unit, e: T) {}
fun <T> baz(e: T, f: (T) -> Unit) {}

fun test(a: A, b: B) {
    // Note that diagnostic is always on callable references as they are resolved after simple arguments
    baz(a, ::<!INAPPLICABLE_CANDIDATE!>fooB<!>)
    bar(::<!INAPPLICABLE_CANDIDATE!>fooB<!>, a)
}
