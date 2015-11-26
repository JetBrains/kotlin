fun <T> doSomething(a: T) {}

fun test(n: Int): String {
    <caret>if (n == 1) {
        doSomething("***")
        return "one"
    }
    return "two"
}
