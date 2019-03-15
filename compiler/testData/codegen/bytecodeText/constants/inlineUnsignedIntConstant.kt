// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND: JVM_IR

// FILE: uint.kt

package kotlin

inline class UInt(val value: Int)

// FILE: test.kt

const val u = UInt(14)

fun foo() {
    u
    if (u.value != 14) {}
}

// @TestKt.class:
// 0 GETSTATIC