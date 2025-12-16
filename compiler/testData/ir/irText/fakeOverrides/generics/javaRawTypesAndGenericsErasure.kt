// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// The test primarily tests reflect dumps (k1 vs new reflect), we don't need kt dumps
// SKIP_KT_DUMP

// FILE: Derived.java
public class Derived extends Base1 implements Base2 {
    public void bar(java.lang.Number a) {}
}

// FILE: Base1.java
public class Base1<R> {
    public void foo(R a) {}
}

// FILE: Base2.java
public interface Base2 {
    public void foo(Object a);

    public <T extends java.lang.Number> void bar(T a);
}
