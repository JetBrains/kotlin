// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun <T> ofType(x: T): T = x

fun foo() {}
fun foo(s: String) {}

val x1 = ofType<() -> Unit>(::foo)
val x2 = ofType<(String) -> Unit>(::foo)
val x3 = ofType<(Int) -> Unit>(::<!NI;DEBUG_INFO_MISSING_UNRESOLVED, OI;NONE_APPLICABLE!>foo<!>)