// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

object X1
object X2

fun <T1> foo(x: T1, f: (T1) -> T1) = X1
fun <T2> foo(xf: () -> T2, f: (T2) -> T2) = X2

val test: X2 = <!CANNOT_COMPLETE_RESOLVE!>foo<!>({ 0 }, { <!CANNOT_INFER_PARAMETER_TYPE!>it<!> -> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>it<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> 1 })