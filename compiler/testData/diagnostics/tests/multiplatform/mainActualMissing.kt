// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect fun main()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

fun <!ACTUAL_MISSING!>main<!>() {}
