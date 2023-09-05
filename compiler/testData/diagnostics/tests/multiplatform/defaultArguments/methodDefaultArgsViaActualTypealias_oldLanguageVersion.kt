// FIR_IDENTICAL
// !DIAGNOSTICS: -ACTUAL_WITHOUT_EXPECT
// LANGUAGE: -MultiplatformRestrictions
// MODULE: m1-common
// FILE: common.kt
expect class A {
    fun foo(p: String = "common")
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

class AImpl {
    fun foo(p: String) {}
}

actual typealias A = AImpl
