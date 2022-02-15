// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    fun foo(i: Int, d: Double, f: Float): Unit
}

// MODULE: m2-jvm()()(m1-common)
// FILE: FooImpl.java

public class FooImpl {
    public final void foo(int d, double i, float f) {}
}

// FILE: jvm.kt

actual typealias Foo = FooImpl
