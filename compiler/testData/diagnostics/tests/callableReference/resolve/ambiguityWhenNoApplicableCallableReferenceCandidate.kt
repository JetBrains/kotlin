// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(x: Int) {}
fun foo(y: String) {}

fun <T> bar(f: (T) -> Unit) {}

fun test() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>bar<!>(::<!CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY, DEBUG_INFO_MISSING_UNRESOLVED!>foo<!>)
}