// IS_APPLICABLE: false
fun doSomething<T>(a: T) {}

fun foo() {
    do {
        doSomething("test")
        doSomething("test2")
    <caret>} while(true)
}
