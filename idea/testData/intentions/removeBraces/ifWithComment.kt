fun doSomething<T>(a: T) {}

fun foo() {
    if (true) <caret>{
        //comment
        doSomething("test")
    }
}
