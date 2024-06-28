class Foo {
    inner class Inner {
        fun foo() {}
    }
}

/**
 * [Foo.Inner.<caret>foo]
 */
fun usage() {}
