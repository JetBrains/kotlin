// "Add '== true'" "false"
// DISABLE-ERRORS
class Foo {
    fun bar() = ""
}

fun test(foo: Foo?) {
    if (foo?.bar()<caret>) {
    }
}
