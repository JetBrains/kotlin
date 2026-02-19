// FILE: Foo.kt
package foo

class Foo {
    fun foo(string: String) {}

    fun foo(number: Int) {}
}

// FILE: main.kt
package test

/**
 * [foo.Foo.<caret>foo]
 */
fun usage() {}
