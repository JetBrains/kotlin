// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun foo() {
    if (true) {
        doSomething("test")
        doSomething("test2")
        <caret>}
}
