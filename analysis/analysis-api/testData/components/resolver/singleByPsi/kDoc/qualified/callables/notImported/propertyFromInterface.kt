// FILE: Foo.kt
package foo

interface Foo {
    val foo: Int
}

// FILE: main.kt
package test

/**
 * [foo.Foo.<caret>foo]
 */
fun usage() {}
