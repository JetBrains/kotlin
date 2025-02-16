// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// MODULE: m1-common
// FILE: common.kt
package common

expect fun foo()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
package jvm

actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>() {}
