fun doSomething<T>(a: T) {}

fun foo() {
    if (true) {
        doSomething("test")
    } <caret>else {
        doSomething("test2")
    }
}
