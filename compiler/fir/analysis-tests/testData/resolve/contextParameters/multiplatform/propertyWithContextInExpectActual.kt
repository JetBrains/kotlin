// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
class A {
    fun foo(): String { return "" }
}

class C

context(a: A)
expect val expectActualMatch : String

expect val expectWithoutContext : String

context(a: A)
expect val actualWithoutContext : String

context(a: A, c: C)
expect val mismatchedContext : String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

context(a: A)
actual val expectActualMatch: String
    get() = a.foo()

context(a: A)
actual val <!ACTUAL_WITHOUT_EXPECT!>expectWithoutContext<!>: String
    get() = a.foo()

actual val <!ACTUAL_WITHOUT_EXPECT!>actualWithoutContext<!>: String
    get() = ""

context(a: A)
actual val <!ACTUAL_WITHOUT_EXPECT!>mismatchedContext<!> : String
    get() = ""
