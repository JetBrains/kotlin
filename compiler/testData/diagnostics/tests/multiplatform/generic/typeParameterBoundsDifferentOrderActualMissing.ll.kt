// MODULE: m1-common
// FILE: common.kt

interface A
interface B

expect fun <T> List<T>.foo() where T : A, T : B

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

fun <T> List<T>.foo() where T : B, T : A {}
