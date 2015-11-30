fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    if (3 > 2) {
        <caret>return when (n) {
            1 -> "one"
            else -> "two"
        }
    } else {
        doSomething("***")
        return "???"
    }
}
