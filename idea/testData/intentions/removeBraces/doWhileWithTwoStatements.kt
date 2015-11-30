// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun foo() {
    do {
        doSomething("test")
        doSomething("test2")
    <caret>} while(true)
}
