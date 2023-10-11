class Foo {
    inner class Inner {
        val foo: Int = 5
    }
}

/**
 * [Foo.Inner.<caret>foo]
 */
fun usage() {}
