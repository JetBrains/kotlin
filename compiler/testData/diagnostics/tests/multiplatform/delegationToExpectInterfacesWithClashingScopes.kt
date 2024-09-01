// MODULE: common
// FILE: common.kt
expect interface <!NO_ACTUAL_FOR_EXPECT!>I<!>
expect interface <!NO_ACTUAL_FOR_EXPECT!>J<!>
<!MANY_IMPL_MEMBER_NOT_IMPLEMENTED{JVM}!>class X<!>(a: I, b : J): I by a, J by b

// MODULE: platform()()(common)
// FILE: platform.kt
actual interface I {
    fun foo()
}
actual interface J {
    fun foo()
}
