// FIR_IDENTICAL
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FILE: A.java
public interface A<T> {
    public void foo(Inv<T> inv);
    public void bar(T t);
}

// FILE: B.java
public interface B<R> extends A<R> {}

// FILE: C.java
public interface C extends B {}

// FILE: main.kt
class Inv<T>(val t: T)
