// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T, R> apply(x: T, f: (T) -> R): R = f(x)

fun foo(i: Int) {}
fun foo(s: String) {}

val x1 = apply(1, ::foo)
val x2 = apply("hello", ::foo)
val x3 = <!TYPE_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>apply<!>(true, ::<!NONE_APPLICABLE!>foo<!>)