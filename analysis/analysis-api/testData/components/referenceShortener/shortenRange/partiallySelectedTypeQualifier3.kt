// FILE: main.kt
package test

fun usage() {
    <expr>dependency.Foo</expr>.Bar.baz()
}

// FILE: dependency.kt
package dependency

object Foo {
    object Bar {
        fun baz() {}
    }
}