// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun foo() {
    if (true) {
        doSomething("test")
        doSomething("test2")
    <caret>}
}
