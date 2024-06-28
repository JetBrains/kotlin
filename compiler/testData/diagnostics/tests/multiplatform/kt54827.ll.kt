// MODULE: m1-common
// FILE: common.kt
expect class SomeClass<T> {
    fun foo()
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class <!ACTUAL_WITHOUT_EXPECT!>SomeClass<!> {
    actual fun foo() {}
}
