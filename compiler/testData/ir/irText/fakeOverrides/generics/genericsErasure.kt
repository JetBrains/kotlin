// WITH_STDLIB
// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// The test primarily tests reflect dumps (k1 vs new reflect), we don't need kt dumps
// SKIP_KT_DUMP

// Disable K1 since it reports: CONFLICTING_OVERLOADS: Conflicting overloads: public final fun <A, B> foo4(a: A): Unit defined in Foo, public final fun <B> foo4(a: B): Unit defined in Foo
// IGNORE_BACKEND_K1: ANY

// K1 reflect thinks that there is only one foo2, new reflect thinks that there are two foo2.
// KT-83380
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: main.kt
class Foo : I1, I2 {
    @JvmName("a1") fun <A> foo1(a: A) where A : Number = Unit
    @JvmName("b1") fun foo1(a: Number) = Unit

    @JvmName("b3") fun <A, B> foo3(a: B) = Unit

    @JvmName("a4") fun <A, B> foo4(a: A) = Unit
    @JvmName("b4") fun <B> foo4(a: B) = Unit
}

// FILE: I1.java
public interface I1 {
    public default <A extends java.io.Serializable & CharSequence> void foo2(A a) {}

    public default <A, B> void foo3(A a) {}
}

// FILE: I2.java
public interface I2 {
    public default <A extends CharSequence & java.io.Serializable> void foo2(A a) {}
}
