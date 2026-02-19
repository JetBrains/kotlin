// FILE: main.kt
package test

import dependency.Foo

fun usage(foo: <expr>dependency.Foo.Bar</expr>.Baz) {}

// FILE: dependency.kt
package dependency

class Foo {
    class Bar {
        class Baz
    }
}