object A {
    fun foo() {
        A.bar()
    }

    fun bar() {
        A.foo()
    }
}