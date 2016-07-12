// IS_APPLICABLE: false

fun Int.foo(x: Int) = this - x

val x = { a: Int, b: Int <caret>-> b.foo(a) }