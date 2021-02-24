// !DIAGNOSTICS: -UNUSED_PARAMETER
// !WITH_NEW_INFERENCE

fun foo(s: String) {}
fun foo(i: Long) {}

fun bar(f: (Boolean) -> Unit) {}

fun test() {
    bar(::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY{NI}, NONE_APPLICABLE{OI}!>foo<!>)
}
