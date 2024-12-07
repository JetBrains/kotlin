// FILE: Foo.kt
package foo

interface Baz {
    fun foo()
}

abstract class Bar : Baz

abstract class Foo : Bar()

// FILE: main.kt
package test

/**
 * [foo.Foo.<caret>foo]
 */
fun usage() {}
