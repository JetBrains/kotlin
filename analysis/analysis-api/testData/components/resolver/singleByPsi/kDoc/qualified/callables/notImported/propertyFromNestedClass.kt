// FILE: Foo.kt
package foo

class Foo {
    class Nested {
        val foo: Int = 5
    }
}

// FILE: main.kt
package test

/**
 * [foo.Foo.Nested.<caret>foo]
 */
fun usage() {}
