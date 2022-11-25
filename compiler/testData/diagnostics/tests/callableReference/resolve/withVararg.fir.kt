// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(vararg ii: Int) {}
fun foo(vararg ss: String) {}
fun foo(i: Int) {}

val fn1: (Int) -> Unit = ::foo
val fn2: (IntArray) -> Unit = ::foo
val fn3: (Int, Int) -> Unit = ::foo
val fn4: (Array<String>) -> Unit = ::foo
