// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

interface A<T> {
    fun foo(x: T)
    fun foo(x: String)

    fun <E> baz(x: E, y: String)
    fun <E> baz(x: String, y: E)
}

fun <E> baz(x: E, y: String) {}
fun <E> baz(x: String, y: E) {}

fun bar(x: A<String>) {
    x.<!AMBIGUITY!>foo<!>("")

    x.<!AMBIGUITY!>baz<!>("", "")
    <!AMBIGUITY!>baz<!>("", "")
}
