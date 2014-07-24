fun doSomething<T>(a: T) {}

fun foo() {
    for<caret> (i in 1..4) {
        doSomething("test")
    }
}
