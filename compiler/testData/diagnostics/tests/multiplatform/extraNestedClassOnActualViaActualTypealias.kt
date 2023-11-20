// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
expect class Foo {
    class Inner
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
class FooImpl {
    class Inner
    class Extra
}

actual typealias Foo = FooImpl
