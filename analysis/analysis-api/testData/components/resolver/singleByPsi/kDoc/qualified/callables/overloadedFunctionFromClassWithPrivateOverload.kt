class Foo {
    fun foo(string: String) {}

    fun foo(number: Int) {}

    private fun foo(boolean: Boolean) {}
}

/**
 * [Foo.<caret>foo]
 */
fun usage() {}
