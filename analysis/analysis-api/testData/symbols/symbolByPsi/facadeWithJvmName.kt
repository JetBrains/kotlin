// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
@file:kotlin.jvm.JvmName("DifferentFacadeName")

fun foo() {}

fun String.foo(): Int = 42

val x: Int = 42

val Int.y get() = this
