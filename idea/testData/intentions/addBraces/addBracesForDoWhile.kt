fun doSomething<T>(a: T) {}

fun foo() {
    <caret>do doSomething("test")
    while(true)
}
