// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

fun <T, R> apply(x: T, f: (T) -> R): R = f(x)

fun foo(i: Int) {}
fun foo(s: String) {}

val x1 = apply(1, ::foo)
val x2 = apply("hello", ::foo)
val x3 = <!NI;NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>apply<!>(true, ::<!NI;CALLABLE_REFERENCE_RESOLUTION_AMBIGUITY, OI;NONE_APPLICABLE!>foo<!>)