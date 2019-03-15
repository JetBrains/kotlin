// PROBLEM: none

<caret>@Deprecated("Use the other version", level=DeprecationLevel.HIDDEN)
fun foo(a: Int) { foo(a) }

fun foo(a: Int, b: Int = 0) { ... }