// IGNORE_BACKEND: JKLIB
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// FULL_JDK

// FILE: Java1.java
public abstract class Java1 extends A {
    // Overrides `CharSequence.get` by its Kotlin name (not `charAt`), which is possible because the Kotlin compiler generates
    // `get(I)C` and a bridge `charAt(I)C` in `A`.
    @Override
    public char get(int index) {
        return 'x';
    }
}

// FILE: Java2.java
public interface Java2 extends CharSequence {
    @Override
    default char charAt(int index) {
        return 'y';
    }
}

// FILE: 1.kt
abstract class A : CharSequence

abstract class B : Java1()  // Kotlin ← Java (override by Kotlin name) ← Kotlin ← CharSequence

abstract class C : A(), Java2   // abstract `get` from A + open `get` (default `charAt`) from Java2

fun test(b: B, c: C) {
    b[1]
    b.get(1)
    c[1]
    c.get(1)
}
