// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// The test primarily tests reflect dumps (k1 vs new reflect), we don't need kt dumps
// SKIP_KT_DUMP

// FILE: main.kt
interface I1 {
    fun foo(a: Int) = Unit
}

class C : I1, I2

class CWithOverride : I1, I2 {
    override fun foo(a: Int) = Unit
}

// FILE: I2.java
public interface I2 {
    public default void foo(Integer i) {}
}
