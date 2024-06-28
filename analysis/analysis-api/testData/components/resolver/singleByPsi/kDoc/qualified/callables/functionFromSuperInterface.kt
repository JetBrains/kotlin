interface Bar {
    fun foo()
}

abstract class Foo : Bar

/**
 * [Foo.<caret>foo]
 */
fun usage() {}
