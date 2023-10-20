// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Repeatable
annotation class AnnWithArg(val s: String)

@AnnWithArg(s = "1")
@AnnWithArg(s = "2")
expect fun diffentOrder()

@AnnWithArg(s = "1")
@AnnWithArg(s = "2")
@AnnWithArg(s = "3")
expect fun withDifferentArgLessOnActual()

@AnnWithArg(s = "1")
@AnnWithArg(s = "3")
expect fun withDifferentArgLessOnExpect()

@AnnWithArg(s = "1")
@AnnWithArg(s = "1")
expect fun withSameArgLessOnActual()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@AnnWithArg(s = "1")
@AnnWithArg(s = "2")
actual fun diffentOrder() {}

@AnnWithArg(s = "1")
@AnnWithArg(s = "3")
actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>withDifferentArgLessOnActual<!>() {}

@AnnWithArg(s = "1")
@AnnWithArg(s = "2")
@AnnWithArg(s = "3")
actual fun withDifferentArgLessOnExpect() {}

@AnnWithArg(s = "1")
actual fun withSameArgLessOnActual() {}
