// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt
interface Shared {
    fun foo(param: Int = 2) {}
}

expect class Foo : Shared

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt
actual class Foo : Shared, A {
    override fun foo(param: Int) {}
}

interface A : B, C {
    override fun foo(param: Int) {}
}

interface B {
    fun foo(param: Int) {}
}

interface C : D, E {
    override fun foo(param: Int) {}
}

interface D {
    fun foo(param: Int) {}
}

interface E {
    fun foo(param: Int) {}
}
