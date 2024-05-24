// FILE: Foo.kt
package foo

open class Bar {
    val foo: Int = 5
}

class Foo : Bar()

// FILE: main.kt
package test

/**
 * [foo.Foo.<caret>foo]
 */
fun usage() {}
