class A {
    fun foo(a: Int, b: String, <caret>c: Any) {

    }
}

class B {
    fun bar(a: A) {
        a.foo(1, "1", "!")
    }
}