// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: -NOTHING_TO_INLINE
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect fun external()<!>
<!CONFLICTING_OVERLOADS!>expect fun tailrec()<!>
<!CONFLICTING_OVERLOADS!>expect fun inline()<!>
<!CONFLICTING_OVERLOADS!>expect fun String.unaryMinus(): String<!>
<!CONFLICTING_OVERLOADS!>expect fun String.and(other: String): String<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual external fun external()
actual tailrec fun tailrec(): Unit = if (true) Unit else tailrec()
actual inline fun inline() {}
actual operator fun String.unaryMinus(): String = this
actual infix fun String.and(other: String): String = this + other
