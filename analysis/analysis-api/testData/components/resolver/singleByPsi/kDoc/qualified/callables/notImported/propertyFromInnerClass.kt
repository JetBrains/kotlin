// FILE: Foo.kt
package foo

class Foo {
    inner class Inner {
        val foo: Int = 5
    }
}

// FILE: main.kt
package test

/**
 * [foo.Foo.Inner.<caret>foo]
 */
fun usage() {}
