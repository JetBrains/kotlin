// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect fun <T : Comparable<T>> Array<out T>.sort(): Unit<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun <T : Comparable<T>> Array<out T>.sort(): Unit {}

fun <T> Array<out T>.sort(): Unit {}
