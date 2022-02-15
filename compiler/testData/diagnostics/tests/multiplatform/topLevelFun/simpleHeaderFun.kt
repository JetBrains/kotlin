// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect fun foo()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual fun foo() {}

// MODULE: m3-js()()(m1-common)
// FILE: js.kt
actual fun foo() {}
