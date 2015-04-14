fun doSomething<T>(a: T) {}

fun foo() {
    if (true) {
        doSomething("test")
    } else <caret>{
        doSomething("test2")
    }
}
