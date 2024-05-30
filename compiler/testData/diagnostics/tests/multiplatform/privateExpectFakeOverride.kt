// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
// ISSUE: KT-68648
abstract class BaseClass(private val x: Int)

expect class ExpectClass : BaseClass {}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class ExpectClass(val x: Int) : BaseClass(x)
