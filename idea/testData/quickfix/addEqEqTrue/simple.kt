// "Add '== true'" "true"
class Foo {
    fun bar() = true
}

fun test(foo: Foo?) {
    if (foo?.bar()<caret>) {
    }
}
