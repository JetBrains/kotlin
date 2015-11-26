// IS_APPLICABLE: false

fun <T> doSomething(a: T) {}

fun test(n: Int) {
    var res: String = ""

    <caret>when (n) {
        1 -> res = "one"
        2 -> res = "two"
        else -> {
            res = "unknown"
            return
        }
    }

    when (n) {
        1 -> doSomething("A")
        2 -> doSomething("B")
        else -> doSomething("C")
    }
}
