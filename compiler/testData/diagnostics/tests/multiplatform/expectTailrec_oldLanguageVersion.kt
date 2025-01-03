// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -MultiplatformRestrictions
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect tailrec fun foo(p: Int): Int<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual tailrec fun foo(p: Int): Int = foo(p)
