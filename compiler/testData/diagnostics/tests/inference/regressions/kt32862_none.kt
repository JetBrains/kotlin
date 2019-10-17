// !DIAGNOSTICS: -UNUSED_PARAMETER
// !LANGUAGE: +NewInference
// !WITH_NEW_INFERENCE

fun foo(s: String) {}
fun foo(i: Long) {}

fun bar(f: (Boolean) -> Unit) {}

fun test() {
    bar(::<!NI;CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY, NI;DEBUG_INFO_MISSING_UNRESOLVED, OI;NONE_APPLICABLE!>foo<!>)
}
