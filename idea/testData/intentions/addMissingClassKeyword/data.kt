// DISABLE-ERRORS
annotation class Ann

@Ann
private data <caret>Foo(val s: String) {
    fun foo() {}
}