// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: other1.kt
package a1
private val X: b.MyEnum get() = TODO()
private val Y: String = ""

// FILE: other2.kt
package a2
private val X: b.MyEnum get() = TODO()
private val Y: String = ""

// FILE: main.kt
package b
import a1.*
import a2.*

enum class MyEnum {
    X, Y, Z
}

fun foo(a: MyEnum) {}

fun main() {
    foo(X)
    foo(Y)
    foo(Z)
}
