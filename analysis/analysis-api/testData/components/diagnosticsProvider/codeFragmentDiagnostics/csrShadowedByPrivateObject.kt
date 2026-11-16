// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// MODULE: other
// FILE: other.kt
private object Red

// MODULE: main(other)
// FILE: main.kt
enum class Color { Red, Green }

fun takeColor(c: Color) {}

fun test() {
    <caret>val x = 0
}
