// FIR_IDENTICAL
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class FooImpl

actual typealias <!ACTUAL_WITHOUT_EXPECT!>Foo<!> = FooImpl
