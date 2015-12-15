class A {
    fun foo(a: Int, <caret>b: String, c: Any) {

    }
}

class B {
    fun bar(a: A) {
        a.foo(1, "1", "!")
    }
}