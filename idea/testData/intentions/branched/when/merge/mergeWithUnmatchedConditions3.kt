// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun test(n: Int) {
    val res: String

    <caret>when (n) {
        1 -> res = "one"
        2 -> res = "two"
        else -> res = "unknown"
    }

    when (n) {
        2 -> doSomething("B")
        1 -> doSomething("A")
        else -> doSomething("C")
    }
}
