// FILE: main.kt

fun <U> test() {}

fun foo() {
    test<<expr>dependency.Foo</expr>>
}

// FILE: dependency.kt
package dependency

interface Foo