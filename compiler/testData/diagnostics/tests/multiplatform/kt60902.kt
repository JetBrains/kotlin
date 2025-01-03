// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>public expect fun <T : Comparable<T>> Array<out T>.foo()<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
public actual fun <T : Comparable<T>> Array<out T>.foo() {
}

private fun <T> Array<out T>.foo() {
}
