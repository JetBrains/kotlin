// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: other.kt
package a
private val X: b.MyEnum get() = TODO()
private val Y: String = ""

// FILE: main.kt
package b
import a.*

enum class MyEnum {
    X, Y, Z
}

fun foo(a: MyEnum) {}

fun main() {
    foo(X)
    foo(Y)
    foo(Z)
}
