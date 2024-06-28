// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Repeatable
annotation class AnnNoArg

@AnnNoArg
@AnnNoArg
expect fun oneMoreOnExpect()

@AnnNoArg
expect fun oneMoreOnActual()

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt

@AnnNoArg
actual fun oneMoreOnExpect() {}

@AnnNoArg
@AnnNoArg
actual fun oneMoreOnActual() {}
