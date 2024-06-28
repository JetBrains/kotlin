// MODULE: common
// FILE: common.kt
expect interface I
expect interface J
class X(a: I, b : J): I by a, J by b

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface I {
    fun foo()
}
actual interface J {
    fun foo()
}
