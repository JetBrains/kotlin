// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +ContextParameters, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
class A
class C

context(a: A)
expect val expectWithContext : String

context(a: A)
expect fun expectWithContext()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

context(a: A)
actual val expectWithContext : String
    get() = ""

context(a: A)
actual fun expectWithContext() {}