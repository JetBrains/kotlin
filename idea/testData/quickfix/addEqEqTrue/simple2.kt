// "Add '== true'" "true"
class Foo {
    fun bar() = true
}

fun baz(b: Boolean) {}

fun test(foo: Foo?) {
    baz(foo?.bar()<caret>)
}
