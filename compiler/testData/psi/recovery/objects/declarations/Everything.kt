object Foo<T, R> private (x: Int, y: Int) : Bar, Baz {
    fun foo() {}
}
// COMPILATION_ERRORS
