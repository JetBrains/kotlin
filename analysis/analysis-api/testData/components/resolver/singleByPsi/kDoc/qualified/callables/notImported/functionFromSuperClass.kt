// FILE: Foo.kt
package foo

open class Bar {
    fun foo() {}
}

class Foo : Bar()

// FILE: main.kt
package test

/**
 * [foo.Foo.<caret>foo]
 */
fun usage() {}
