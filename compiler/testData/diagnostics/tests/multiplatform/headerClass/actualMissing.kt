// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class A {
    fun foo()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

class <!ACTUAL_MISSING!>A<!> {
    actual fun foo() {}
}
