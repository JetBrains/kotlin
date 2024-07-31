// ISSUE: KT-61506
// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// MODULE: m1-common
// FILE: common.kt

expect fun run(r: () -> Unit)

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

actual inline fun run(r: () -> Unit) = r()

fun test() {
    run {
        return
    }
}