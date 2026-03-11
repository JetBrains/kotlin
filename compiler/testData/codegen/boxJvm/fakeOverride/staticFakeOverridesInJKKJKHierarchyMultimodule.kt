// TARGET_BACKEND: JVM_IR
// ISSUE: KT-68191

// MODULE: a
// FILE: A_J.java
public abstract class A_J {
    public static void foo() {}
}

// MODULE: b(a)
// FILE: B_K.kt
abstract class B_K : A_J()

// MODULE: c(b, a)
// FILE: C_K.kt
abstract class C_K : B_K()

// FILE: D_J.java
public abstract class D_J extends C_K {}

// FILE: E_K.kt
class E_K : D_J()

fun box() = "OK"
