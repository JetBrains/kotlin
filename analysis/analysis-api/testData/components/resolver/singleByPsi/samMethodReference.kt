class Test {
    fun interface Foo {
        fun foo()
    }

    fun uiMethod() {}

    fun test(foo: Foo) {}

    fun testMethodRef() {
        <expr>test(this::uiMethod)</expr>
    }
}