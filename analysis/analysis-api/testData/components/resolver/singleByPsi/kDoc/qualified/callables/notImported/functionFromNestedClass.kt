// FILE: Foo.kt
package foo

class Foo {
    class Nested {
        fun foo() {}
    }
}

// FILE: main.kt
package test

/**
 * [foo.Foo.Nested.<caret>foo]
 */
fun usage() {}
