class Foo {
    class Nested {
        val foo: Int = 5
    }
}

/**
 * [Foo.Nested.<caret>foo]
 */
fun usage() {}
