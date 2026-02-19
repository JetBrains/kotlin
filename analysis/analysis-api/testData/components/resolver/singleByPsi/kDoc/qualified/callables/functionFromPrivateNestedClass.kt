class Foo {
    private class Nested {
        fun foo() {}
    }
}

/**
 * [Foo.Nested.<caret>foo]
 */
fun usage() {}
