// IGNORE_BACKEND: JKLIB
// SKIP_KT_DUMP
// TARGET_BACKEND: JVM
// WITH_STDLIB

// This test checks behavior when classes have same names as renamed builtins (`toByte`, `byteValue`), but DO NOT inherit from `Number`.

// New implementation sees both `foo`s (one with JVM name `fooJvm`, another with JVM name `foo`), which is arguably more correct than K1.
// KOTLIN_REFLECT_DUMP_MISMATCH

// FILE: Java1.java
public interface Java1 {
    byte toByte();

    int toInt();

    void foo();
}

// FILE: Java2.java
public abstract class Java2 extends A {
    public byte toByte() { return 3; }

    public void foo() { }
}

// FILE: 1.kt
abstract class A {
    @JvmName("byteValue")
    fun toByte(): Byte = 1

    @JvmName("intValue")
    fun toInt(): Int = 2

    @JvmName("fooJvm")
    fun foo() { }
}

abstract class B : A()   // Kotlin ← Kotlin (@JvmName)

abstract class C : A(), Java1   // Kotlin ← Kotlin (@JvmName), Java (toByte, toInt, foo)

abstract class D : Java2()   // Kotlin ← Java (override by Kotlin name) ← Kotlin (@JvmName)

fun test(b: B, c: C, d: D) {
    b.toByte()
    b.toInt()
    b.foo()

    c.toByte()
    c.toInt()
    c.foo()

    d.toByte()
    d.toInt()
    d.foo()
}
