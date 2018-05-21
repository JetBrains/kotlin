class Test {
    private val foo = 1

    fun <caret>Test.bar() = foo + this.foo + this@Test.foo + this@bar.foo
}
