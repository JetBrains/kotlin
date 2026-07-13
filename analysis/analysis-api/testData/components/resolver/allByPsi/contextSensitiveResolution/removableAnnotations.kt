// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: main.kt
package main

enum class Foo { BAR }

annotation class Anno(val f: Foo)

@Anno(Foo.BAR)
fun usage() {
}
