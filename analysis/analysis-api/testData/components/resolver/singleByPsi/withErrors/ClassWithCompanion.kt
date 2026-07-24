class A<T> {
    companion object
}

fun test() {
    <caret>A.foo(2)
}
