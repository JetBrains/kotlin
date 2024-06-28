// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect annotation class A1(val x: Int, val y: String = "OK")

expect annotation class A2(val x: Int = 42, val y: String = "OK")

expect annotation class A3(val x: Int, val y: String)

expect annotation class A4(val x: Int = 42, val y: String)

expect annotation class A5(val x: Int = 42, val y: String)

@A1(0)
@A2
@A3(0, "")
@A4(0, "")
@A5(0, "")
fun test() {}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual annotation class A1(actual val x: Int, actual val y: String)

actual annotation class A2(actual val x: Int, actual val y: String = "OK")

actual annotation class A3(actual val x: Int = 42, actual val y: String = "OK")

actual annotation class A4(actual val x: Int, actual val y: String = "OK")

actual annotation class A5(actual val x: Int = <!ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE!>239<!>, actual val y: String = "OK")
