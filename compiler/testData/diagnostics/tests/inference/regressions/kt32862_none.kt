// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(s: String) {}
fun foo(i: Long) {}

fun bar(f: (Boolean) -> Unit) {}

fun test() {
    bar(::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY!>foo<!>)
}
