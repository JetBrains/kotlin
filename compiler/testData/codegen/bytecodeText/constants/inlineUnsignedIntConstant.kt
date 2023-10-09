// !LANGUAGE: +InlineClasses
// ALLOW_KOTLIN_PACKAGE
// JVM_ABI_K1_K2_DIFF: KT-62750

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