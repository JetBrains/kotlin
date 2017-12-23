// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// NI_EXPECTED_FILE

fun <T, R> apply(x: T, f: (T) -> R): R = f(x)

fun foo(i: Int) {}
fun foo(s: String) {}

val x1 = apply(1, ::foo)
val x2 = apply("hello", ::foo)
val <!NI;IMPLICIT_NOTHING_PROPERTY_TYPE!>x3<!> = <!OI;TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>apply<!>(true, ::<!NI;DEBUG_INFO_MISSING_UNRESOLVED, OI;NONE_APPLICABLE!>foo<!>)