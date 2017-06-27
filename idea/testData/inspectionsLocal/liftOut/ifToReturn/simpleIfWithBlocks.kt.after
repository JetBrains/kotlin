fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    <caret>return if (n == 1) {
        doSomething("***")
        "one"
    } else {
        doSomething("***")
        "two"
    }
}
