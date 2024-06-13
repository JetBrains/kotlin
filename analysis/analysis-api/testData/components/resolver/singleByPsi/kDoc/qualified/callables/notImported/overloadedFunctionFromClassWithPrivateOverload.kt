// FILE: Foo.kt
package foo

class Foo {
    fun foo(string: String) {}

    fun foo(number: Int) {}

    private fun foo(boolean: Boolean) {}
}

// FILE: main.kt
package test

/**
 * [foo.Foo.<caret>foo]
 */
fun usage() {}
