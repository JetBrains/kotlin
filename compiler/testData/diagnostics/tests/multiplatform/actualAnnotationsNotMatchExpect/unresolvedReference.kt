// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNRESOLVED_REFERENCE
// MODULE: m1-common
// FILE: common.kt
@NonExistingClass
expect fun foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun foo() {}
