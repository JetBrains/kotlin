// !LANGUAGE: +InlineClasses

// FILE: uint.kt

package kotlin

inline class UInt(val value: Int)

// FILE: test.kt

//this import in required in FIR because default imports don't search in local sources atm
import kotlin.UInt

const val u = UInt(14)

fun foo() {
    u
    if (u.value != 14) {}
}

// @TestKt.class:
// 0 GETSTATIC