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
actual val expectWithoutContext: String
    get() = ""

actual val actualWithoutContext: String
    get() = ""

context(a: A)
actual val mismatchedContext : String
    get() = ""

context(a: C)
actual val wrongContextType : String
    get() = ""

context(c: A)
actual val wrongContextName : String
    get() = ""