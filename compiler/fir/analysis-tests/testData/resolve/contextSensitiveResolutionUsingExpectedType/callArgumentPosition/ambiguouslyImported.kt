// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75315
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: other1.kt
package a1
val X: b.MyEnum get() = TODO()
val Y: String = ""

// FILE: other2.kt
package a2
val X: b.MyEnum get() = TODO()
val Y: String = ""

// FILE: main.kt
package b
import a1.*
import a2.*

enum class MyEnum {
    X, Y, Z
}

fun foo(a: MyEnum) {}

fun main() {
    foo(<!OVERLOAD_RESOLUTION_AMBIGUITY!>X<!>)
    foo(<!OVERLOAD_RESOLUTION_AMBIGUITY!>Y<!>)
    foo(Z)
}
