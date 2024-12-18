// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
class A
class C

expect val expectActualMatch: context(A)() -> Unit

expect val expectWithoutContext: () -> Unit

expect val actualWithoutContext: context(A)() -> Unit

expect val mismatchedContext: context(A, C)() -> Unit

expect val expectContextActualExtension: context(A)() -> Unit

expect val expectExtensionActualContext : A.() -> Unit

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual val expectActualMatch : context(A)()->Unit = { }

actual val <!ACTUAL_WITHOUT_EXPECT!>expectWithoutContext<!> : context(A)()->Unit = { }

actual val <!ACTUAL_WITHOUT_EXPECT!>actualWithoutContext<!> : ()->Unit = { }

actual val <!ACTUAL_WITHOUT_EXPECT!>mismatchedContext<!>: context(A)() -> Unit = { }

actual val expectContextActualExtension: A.() -> Unit = { }

actual val expectExtensionActualContext : context(A)() -> Unit = { }
