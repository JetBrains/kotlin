// FIR_IDENTICAL
// MODULE: m1-common
// FILE: common.kt

expect interface Foo {
    fun foo(param: String)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Base.java
public interface I {
    public void foo(String renamed) {}
}

// FILE: jvm.kt
actual interface Foo : I {
    actual override fun foo(param: String)
}
