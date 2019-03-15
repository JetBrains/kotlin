fun <T> doSomething(a: T) {}

fun test(a: Int) {
    <caret>when (a) {
        0 -> doSomething("A")
        1 -> doSomething("B")
    };

    when (a) {
        0 -> doSomething("C")
        1 -> doSomething("D")
    }
}