open class Bar {
    fun foo() {}
}

class Foo : Bar()

/**
 * [Foo.<caret>foo]
 */
fun usage() {}
