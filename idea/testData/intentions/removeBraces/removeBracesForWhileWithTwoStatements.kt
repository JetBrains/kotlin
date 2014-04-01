// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun foo() {
    while (true) {
        doSomething("test")
        doSomething("test2")
        <caret>}
}
