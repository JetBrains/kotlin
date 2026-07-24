// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// FILE: main.kt
package usage

fun expectsSealed(s: pkg.MySealedClass) {}

fun test() {
    expectsSealed(<expr>pkg.MySealedClass</expr>.SubObject)
}

// FILE: dependency.kt
package pkg

sealed class MySealedClass {
    data object SubObject : MySealedClass()
}
