// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
class A

context(a: A)
expect val expectContextActualExtension : String

expect val A.expectExtensionActualContext : String

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual val A.<!ACTUAL_WITHOUT_EXPECT!>expectContextActualExtension<!> : String
    get() = ""

context(a: A)
actual val <!ACTUAL_WITHOUT_EXPECT!>expectExtensionActualContext<!> : String
    get() = ""
