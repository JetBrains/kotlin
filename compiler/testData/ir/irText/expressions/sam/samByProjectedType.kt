// FIR_IDENTICAL
// TARGET_BACKEND: JVM

// NO_SIGNATURE_DUMP
// ^KT-57428

// FILE: samByProjectedType.kt
fun test1() {
    H.bar { x: Any -> x }
}

// FILE: J.java
public interface J<T> {
    T foo(T x);
}

// FILE: H.java
public class H {
    public static void bar(J<?> j) {}
}
