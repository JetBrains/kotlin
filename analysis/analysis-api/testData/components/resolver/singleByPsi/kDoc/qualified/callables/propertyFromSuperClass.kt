open class Bar {
    val foo: Int = 5
}

class Foo : Bar()

/**
 * [Foo.<caret>foo]
 */
fun usage() {}
