// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

interface A<T> {
    fun foo(x: T)
    fun foo(x: String)

    fun <E> baz(x: E, y: String)
    fun <E> baz(x: String, y: E)
}

fun <E> baz(x: E, y: String) {}
fun <E> baz(x: String, y: E) {}

fun bar(x: A<String>) {
    x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>("")

    x.<!OVERLOAD_RESOLUTION_AMBIGUITY!>baz<!>("", "")
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>baz<!>("", "")
}
