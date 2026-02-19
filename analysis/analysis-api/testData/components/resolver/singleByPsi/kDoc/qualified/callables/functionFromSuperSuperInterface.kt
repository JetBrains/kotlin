interface Baz {
    fun foo()
}

abstract class Bar : Baz

abstract class Foo : Bar()

/**
 * [Foo.<caret>foo]
 */
fun usage() {}
