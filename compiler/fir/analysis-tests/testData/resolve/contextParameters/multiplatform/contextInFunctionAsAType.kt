// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters, +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt
class A
class C

expect fun typeMatch(a: context(A)() -> Unit): context(A) () -> Unit

expect fun actualWithoutContext(a: context(A)() -> Unit): context(A) () -> Unit

expect fun expectWithoutContext(a: () -> Unit): () -> Unit

expect fun mismatchedContext(a: context(A)() -> Unit): context(A) () -> Unit

expect fun expectContextActualExtension(a: context(A) () -> Unit): context(A) () -> Unit

expect fun expectContextActualValueParam(a: context(A) () -> Unit): context(A) () -> Unit

expect fun expectValueParamActualContext(a: (A) -> Unit): (A) -> Unit

expect fun expectExtensionActualContext(a: A.() -> Unit): A.() -> Unit

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun typeMatch(a: context(A)() -> Unit): context(A) () -> Unit = a

actual fun <!ACTUAL_WITHOUT_EXPECT!>actualWithoutContext<!>(a: () -> Unit): () -> Unit = a

actual fun <!ACTUAL_WITHOUT_EXPECT!>expectWithoutContext<!>(a: context(A)() -> Unit): context(A)() -> Unit = a

actual fun <!ACTUAL_WITHOUT_EXPECT!>mismatchedContext<!>(a: context(A, C)() -> Unit): context(A, C) () -> Unit = a

actual fun expectContextActualExtension(a: A.() -> Unit): A.() -> Unit = a

actual fun expectContextActualValueParam(a: (A) -> Unit): (A) -> Unit = a

actual fun expectValueParamActualContext(a: context(A)() -> Unit): context(A) () -> Unit = a

actual fun expectExtensionActualContext(a: context(A) () -> Unit): context(A)() -> Unit = a

