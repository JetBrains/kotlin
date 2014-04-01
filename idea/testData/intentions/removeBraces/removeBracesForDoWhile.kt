fun doSomething<T>(a: T) {}

fun foo() {
    do {
        doSomething("test")
    <caret>} while(true)
}
