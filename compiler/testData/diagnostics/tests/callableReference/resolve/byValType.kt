// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {}
fun foo(s: String) {}

val x1 = ::<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>
val x2: () -> Unit = ::foo
val x3: (String) -> Unit = ::foo
val x4: (Int) -> Unit = ::<!NONE_APPLICABLE!>foo<!>