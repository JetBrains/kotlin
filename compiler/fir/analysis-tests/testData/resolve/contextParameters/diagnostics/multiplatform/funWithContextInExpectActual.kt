// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

class A
class C

context(a: A)
expect fun expectActualMatch()

context(a: A)
expect fun actualWithoutContext()

expect fun expectWithoutContext()

context(a: A, c: C)
expect fun mismatchedContext()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

context(a: A)
actual fun expectActualMatch() { }

actual fun <!ACTUAL_WITHOUT_EXPECT!>actualWithoutContext<!>() { }

context(a: A)
actual fun <!ACTUAL_WITHOUT_EXPECT!>expectWithoutContext<!>() { }

context(a: A)
actual fun <!ACTUAL_WITHOUT_EXPECT!>mismatchedContext<!>() { }
