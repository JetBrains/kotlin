// FIR_IDENTICAL
// !DIAGNOSTICS: -NOTHING_TO_INLINE
// MODULE: m1-common
// FILE: common.kt

expect fun external()
expect fun tailrec()
expect fun inline()
expect fun String.unaryMinus(): String
expect fun String.and(other: String): String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual external fun external()
actual tailrec fun tailrec(): Unit = if (true) Unit else tailrec()
actual inline fun inline() {}
actual operator fun String.unaryMinus(): String = this
actual infix fun String.and(other: String): String = this + other
