// "Create class 'Foo'" "true"

fun test() {
    fun nestedTest() = <caret>Foo(2, "2")
}