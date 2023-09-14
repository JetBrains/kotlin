interface Bar {
    val foo: Int
}

abstract class Foo : Bar

/**
 * [Foo.<caret>foo]
 */
fun usage() {}
