// IDE_MODE
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType
// FILE: main.kt
package usage

fun expectsMyEnum(e: pkg.MyEnum) {}

fun test() {
    expectsMyEnum(<expr>pkg.MyEnum</expr>.A)
}

// FILE: dependency.kt
package pkg

enum class MyEnum {
    A, B
}
