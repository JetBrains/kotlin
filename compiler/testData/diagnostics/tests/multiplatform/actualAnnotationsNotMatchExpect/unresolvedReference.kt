// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
@<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE{JVM}!>NonExistingClass<!>
expect fun foo()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual fun foo() {}
