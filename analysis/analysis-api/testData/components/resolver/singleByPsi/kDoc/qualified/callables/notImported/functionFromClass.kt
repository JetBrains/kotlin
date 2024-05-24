// FILE: Foo.kt
package foo

class Foo {
    fun foo() {}
}

// FILE: main.kt
package test

/**
 * [foo.Foo.<caret>foo]
 */
fun usage() {}
