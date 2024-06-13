// FILE: Foo.kt
package foo

interface Bar {
    val foo: Int
}

abstract class Foo : Bar

// FILE: main.kt
package test

/**
 * [foo.Foo.<caret>foo]
 */
fun usage() {}
