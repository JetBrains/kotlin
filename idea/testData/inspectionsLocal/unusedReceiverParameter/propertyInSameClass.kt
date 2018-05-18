class Test {
    private val foo = 1

    val <caret>Test.baz: Int
        get() = foo + foo
}
