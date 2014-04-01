fun doSomething<T>(a: T) {}

fun foo() {
    <caret>while (true)
        doSomething("test")
}
