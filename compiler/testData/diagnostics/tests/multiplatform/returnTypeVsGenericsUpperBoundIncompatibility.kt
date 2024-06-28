// MODULE: m1-common
// FILE: common.kt

interface A
expect fun <T : A> foo(t: T): <!NO_ACTUAL_FOR_EXPECT{JVM}!>String<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

fun <T : A> foo(t: T): T = TODO()
fun <T> foo(t: T): String = TODO()
