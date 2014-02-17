// IS_APPLICABLE: false
class Foo {
    fun foo(x: Int = 0, y: Int = 0) {
        println("lol")
    }
}

fun bar(baz: Foo) {
    baz.<caret>foo(y = 1)
}