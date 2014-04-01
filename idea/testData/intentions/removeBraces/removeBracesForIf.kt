fun doSomething<T>(a: T) {}

fun foo() {
    <caret>if (true) {
        doSomething("test")
    }
}
