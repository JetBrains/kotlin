// FILE: main.kt
package test

fun usage() {
    <expr>dependency</expr>.Foo.bar()
}

// FILE: dependency.kt
package dependency

object Foo {
    fun bar() {}
}