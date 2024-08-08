// MODULE: common
// FILE: common.kt
expect interface <!NO_ACTUAL_FOR_EXPECT!>Base<!><T> {
    fun foo():T
}
class DelegatedImpl<T : Base<T>>(val a: T) : Base<T> by a

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface Base<T> {
    actual fun foo(): T
    fun bar():T
}
