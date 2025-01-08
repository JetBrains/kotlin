// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
class A
class C

expect val expectWithoutContext : String

context(a: A)
expect val actualWithoutContext : String

context(a: A, c: C)
expect val mismatchedContext : String

context(a: A)
expect val wrongContextType : String

context(a: A)
expect val wrongContextName : String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

context(a: A)
actual val <!ACTUAL_WITHOUT_EXPECT!>expectWithoutContext<!>: String
    get() = ""

actual val <!ACTUAL_WITHOUT_EXPECT!>actualWithoutContext<!>: String
    get() = ""

context(a: A)
actual val <!ACTUAL_WITHOUT_EXPECT!>mismatchedContext<!> : String
    get() = ""

context(a: C)
actual val <!ACTUAL_WITHOUT_EXPECT!>wrongContextType<!> : String
    get() = ""

context(c: A)
actual val <!ACTUAL_WITHOUT_EXPECT!>wrongContextName<!> : String
    get() = ""
