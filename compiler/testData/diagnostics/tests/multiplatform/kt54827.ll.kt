// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
expect class SomeClass<T> {
    fun foo()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_TYPE_PARAMETER_COUNT!>SomeClass<!> {
    actual fun foo() {}
}
