class A {
    fun B.foo() {}
}

interface B

fun bar(a: A, b: B) {
    with (a) {
        with (b) {
            <caret>foo()
        }
    }
}
