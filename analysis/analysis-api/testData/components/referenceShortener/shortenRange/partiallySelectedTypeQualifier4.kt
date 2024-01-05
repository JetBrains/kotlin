// FILE: main.kt
package test

import dependency.Foo

fun usage() {
    <expr>dependency.Foo.Bar</expr>.Baz.qux()
}

// FILE: dependency.kt
package dependency

object Foo {
    class Bar {
        object Baz {
            fun qux() {}
        }
    }
}