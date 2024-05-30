// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
// ISSUE: KT-68674
abstract class BaseClass(private val x: String)

expect class ExpectClass : BaseClass {}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class ExpectClass(val x: Int) : BaseClass(x.toString())
