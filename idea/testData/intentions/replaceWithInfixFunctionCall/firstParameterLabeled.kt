class Foo {
    fun foo(x: Int) {
        println("lol")
    }
}

fun bar(baz: Foo) {
    baz.<caret>foo(x = 1)
}