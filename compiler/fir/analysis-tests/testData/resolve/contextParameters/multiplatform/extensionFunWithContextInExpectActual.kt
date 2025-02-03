// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
class A

context(a: A)
expect fun expectContextActualExtension()

context(a: A)
expect fun expectContextActualValueParam()

expect fun expectValueParamActualContext(a: A)

expect fun A.expectExtensionActualContext()

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun A.<!ACTUAL_WITHOUT_EXPECT!>expectContextActualExtension<!>() { }

actual fun <!ACTUAL_WITHOUT_EXPECT!>expectContextActualValueParam<!>(a: A) { }

context(a: A)
actual fun <!ACTUAL_WITHOUT_EXPECT!>expectValueParamActualContext<!>() { }

context(a: A)
actual fun <!ACTUAL_WITHOUT_EXPECT!>expectExtensionActualContext<!>() { }

