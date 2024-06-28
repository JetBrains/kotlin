// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-68688

// MODULE: m1-common
// FILE: common.kt

// Value classes can't be written without a constructor. That's why `private constructor` is allowed
// Oh, private properties of value classes are allowed as well for the same reason. Have fun
expect value class Value private constructor(private val x: Int)

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

@JvmInline
actual value class Value public constructor(val x: Int)
