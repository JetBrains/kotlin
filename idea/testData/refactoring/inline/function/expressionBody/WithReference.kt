
fun <caret>foo(x: Int) = x

val y = ::foo

fun bar(x: Int) = foo(x * x)