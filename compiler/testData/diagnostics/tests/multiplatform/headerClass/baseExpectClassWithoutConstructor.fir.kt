// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: m1-common
// FILE: common.kt

expect open class A
expect class B : A
open <!UNRESOLVED_REFERENCE!>class C<!> : <!SUPERTYPE_NOT_INITIALIZED!>A<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual open class A
actual class B : A()
