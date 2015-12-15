class A {
    fun foo(<caret>a: Int, b: String, c: Any) {
        println(b)
    }
}

class B {
    fun bar(a: A) {
        a.foo(1, "1", "!")
    }
}