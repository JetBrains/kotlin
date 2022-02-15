// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect interface A<T> {
    val x: T
    var y: List<T>
    fun f(p: Collection<T>): Map<T, A<T?>>
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual interface A<T> {
    actual val x: T
    actual var y: List<T>
    actual fun f(p: Collection<T>): Map<T, A<T?>>
}
