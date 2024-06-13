interface Baz {
    val foo: Int
}

abstract class Bar : Baz

abstract class Foo : Bar()

/**
 * [Foo.<caret>foo]
 */
fun usage() {}
