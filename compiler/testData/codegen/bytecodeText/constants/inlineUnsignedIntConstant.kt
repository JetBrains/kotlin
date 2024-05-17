// LANGUAGE: +InlineClasses
// ALLOW_KOTLIN_PACKAGE

// FILE: uint.kt
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
package kotlin

inline class UInt @kotlin.internal.IntrinsicConstEvaluation constructor(val value: Int)

// FILE: test.kt

const val u = UInt(14)

fun foo() {
    u
    if (u.value != 14) {}
}

// @TestKt.class:
// 0 GETSTATIC
