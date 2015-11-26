// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun test() {
    var n = 0

    <caret>when (n) {
        1 -> n = 2
        2 -> n = 1
    }

    when (n) {
        1 -> doSomething("A")
        2 -> doSomething("B")
    }
}
