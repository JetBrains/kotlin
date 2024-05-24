// FILE: Foo.kt
package foo

class Foo {
    val foo: Int = 5
}

// FILE: main.kt
package test

/**
 * [foo.Foo.<caret>foo]
 */
fun usage() {}
