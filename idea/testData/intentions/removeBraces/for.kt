fun doSomething<T>(a: T) {}

fun foo() {
    for (i in 1..4) {<caret>
        doSomething("test")
    }
}
