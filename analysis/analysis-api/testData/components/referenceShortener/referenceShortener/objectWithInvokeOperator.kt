// FILE: main.kt
package test

fun test() {
    <expr>dependency.Foo()</expr>
}

// FILE: dependency.kt
package dependency

object Foo {
    operator fun invoke() {}
}

