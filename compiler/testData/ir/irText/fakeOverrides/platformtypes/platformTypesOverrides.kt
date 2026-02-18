// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// The test primarily tests reflect dumps (k1 vs new reflect), we don't need kt dumps
// SKIP_KT_DUMP

// FILE: J1.java
public interface J1 {
    public default void foo(Integer a) {}
}

// FILE: J2.java
public interface J2 {
    public default void foo(int a) {}
}

// FILE: J3.java
public class J3 extends C {
    public void foo(int a) {}
}

// FILE: main.kt
open class C : J1, J2 {
    override fun foo(a: Int?) {
    }
}
