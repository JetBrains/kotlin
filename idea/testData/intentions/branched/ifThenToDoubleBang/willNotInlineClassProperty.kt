// WITH_RUNTIME
class F(a: Int?) {
    val b = a
    val c = if (b != <caret> null) b else throw NullPointerException()
}

fun main(args: Array<String>) {
    F(1).c
}
