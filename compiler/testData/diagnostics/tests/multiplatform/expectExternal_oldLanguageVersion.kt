// FIR_IDENTICAL
// LANGUAGE: -MultiplatformRestrictions
// MODULE: m1-common
// FILE: common.kt

expect external fun foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual external fun foo()
