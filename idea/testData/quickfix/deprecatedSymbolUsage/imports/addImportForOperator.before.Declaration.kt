package declaration

fun <T> listOf(element: T): List<T> = throw Exception()

@Deprecated("", ReplaceWith("newFun(n + listOf(s))", "weird.collections.plus"))
fun oldFun(n: List<Int>, s: Int) {}

fun newFun(n: List<Int>) {}
