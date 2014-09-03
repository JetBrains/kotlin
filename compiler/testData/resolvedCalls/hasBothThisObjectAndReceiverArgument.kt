class A {
    fun B.foo() {}
}

trait B

fun bar(a: A, b: B) {
    with (a) {
        b.<caret>foo()
    }
}