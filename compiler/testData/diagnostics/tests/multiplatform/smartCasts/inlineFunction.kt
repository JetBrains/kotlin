// FIR_IDENTICAL
// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-61506
// MODULE: m1-common
// FILE: common.kt

expect fun run(r: () -> Unit)

fun testCommon() {
    run {
        // K1: compiled, K2: 'return' is not allowed here
        <!RETURN_NOT_ALLOWED!>return<!>
    }
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual inline fun run(r: () -> Unit) = r()

fun testPlatform() {
    run {
        return
    }
}
