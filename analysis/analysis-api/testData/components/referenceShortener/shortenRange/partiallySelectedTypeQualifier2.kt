// FILE: main.kt
package test

fun usage() {
    dependency.<expr>Foo</expr>.bar()
}

// FILE: dependency.kt
package dependency

object Foo {
    fun bar() {}
}