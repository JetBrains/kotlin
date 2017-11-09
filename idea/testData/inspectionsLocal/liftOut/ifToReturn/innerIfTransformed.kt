fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    if (n == 1) {
        <caret>if (3 > 2) {
            doSomething("***")
            return "one"
        } else {
            doSomething("***")
            return "???"
        }
    } else if (n == 2) {
        doSomething("***")
        return "two"
    } else {
        doSomething("***")
        return "too many"
    }
}
