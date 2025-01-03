// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt
<!CONFLICTING_OVERLOADS!>expect fun foo()<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual fun foo() {}
