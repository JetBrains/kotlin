interface Foo {
    fun function()
}

fun foo(parameter: Any) {
    if (parameter is Foo) {
        <caret>parameter.function()
    }
}
