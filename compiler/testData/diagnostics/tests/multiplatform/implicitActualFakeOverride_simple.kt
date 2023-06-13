// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    fun foo(): Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

interface FooProvider {
    fun foo(): Int = 42
}

actual class Foo : FooProvider
