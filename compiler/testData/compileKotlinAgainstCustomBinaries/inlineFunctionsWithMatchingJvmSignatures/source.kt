import lib.*

fun run() {
    val test1 = 42.toString(10)
    val test2 = J(42).toString(10)
    if (test1 != "42") throw AssertionError(test1)
    if (test2 != "J42") throw AssertionError(test2)
}