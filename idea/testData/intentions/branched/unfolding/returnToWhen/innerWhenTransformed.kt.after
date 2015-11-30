fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    if (3 > 2) {
        <caret>when (n) {
            1 -> return "one"
            else -> return "two"
        }
    } else {
        doSomething("***")
        return "???"
    }
}
