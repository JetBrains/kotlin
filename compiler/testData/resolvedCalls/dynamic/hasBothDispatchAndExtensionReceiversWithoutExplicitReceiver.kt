class A {
    fun dynamic.foo() {}
}

fun bar(a: A, b: dynamic) {
    with (a) {
        with (b) {
            <caret>foo()
        }
    }
}
