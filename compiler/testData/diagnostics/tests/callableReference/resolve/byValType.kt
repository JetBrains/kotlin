// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {}
fun foo(s: String) {}

val x1 = ::foo
val x2: () -> Unit = ::foo
val x3: (String) -> Unit = ::foo
val x4: (Int) -> Unit = <!TYPE_MISMATCH!>::foo<!>