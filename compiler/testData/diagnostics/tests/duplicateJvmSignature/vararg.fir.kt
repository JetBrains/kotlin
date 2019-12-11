fun foo(vararg x: Int) {}
fun foo(x: IntArray) {}

fun foo(vararg x: Int?) {}
fun foo(x: Array<Int>) {}

fun foo(vararg nn: Number) {}
fun foo(nn: Array<out Number>) {}
