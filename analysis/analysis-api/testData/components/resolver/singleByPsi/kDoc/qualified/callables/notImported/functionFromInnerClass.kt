// FILE: Foo.kt
package foo

class Foo {
    inner class Inner {
        fun foo() {}
    }
}

// FILE: main.kt
package test

/**
 * [foo.Foo.Inner.<caret>foo]
 */
fun usage() {}
