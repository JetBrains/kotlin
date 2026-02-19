// FILE: Foo.kt
package foo

interface Foo {
    fun foo()
}

// FILE: main.kt
package test

/**
 * [foo.Foo.<caret>foo]
 */
fun usage() {}
