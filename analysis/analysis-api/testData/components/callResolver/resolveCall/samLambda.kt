class Test {
    fun interface Foo {
        fun foo()
    }

    fun uiMethod() {}

    fun test(foo: Foo) {}

    fun testLambda() {
        <expr>test { uiMethod() }</expr>
    }
}