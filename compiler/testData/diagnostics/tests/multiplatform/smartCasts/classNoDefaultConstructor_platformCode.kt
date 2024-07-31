// ISSUE: KT-61506
// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

package pack

expect class Bar {
    fun foo(): String
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

package pack

actual class Bar {
    actual fun foo() = "expect class fun: jvm"
}

fun common() {
    Bar().foo()
}