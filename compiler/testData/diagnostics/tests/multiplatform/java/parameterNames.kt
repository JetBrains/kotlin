// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class Foo {
    fun foo(i: Int, d: Double, f: Float): Unit
}

// MODULE: m2-jvm(m1-common)
// FILE: FooImpl.java

public class FooImpl {
    public final void foo(int d, double i, float f) {}
}

// FILE: jvm.kt

impl typealias Foo = FooImpl
