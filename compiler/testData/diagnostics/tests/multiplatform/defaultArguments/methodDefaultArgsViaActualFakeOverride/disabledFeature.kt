// FIR_IDENTICAL
// LANGUAGE: -ProhibitDefaultArgumentsInExpectActualizedByFakeOverride
// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    fun foo(param: Int = 1)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
open class Base {
    fun foo(param: Int) {}
}

actual class Foo : Base()
