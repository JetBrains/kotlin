// FIR_IDENTICAL
// ISSUE: KT-62024
// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    fun foo(): Int
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

interface I {
    fun foo() = 1
}

actual class Foo(i: I) : I by i

fun test(foo: Foo) {
    foo.foo()
}
