// TARGET_BACKEND: JVM_IR
// JVM_TARGET: 1.8
// ISSUE: KT-68191

// MODULE: a
// FILE: Trait_A.java
public interface Trait_A {
    public static String foo() { return "Trait A"; }
}

// FILE: A_J.java
public abstract class A_J {
    public static String foo() { return "OK"; }
}

// MODULE: b(a)
// FILE: Trait_B.java
public interface Trait_B {
    public static String foo() { return "Trait B"; }
}

// FILE: B_K.kt
abstract class B_K : A_J(), Trait_A

// MODULE: c(b, a)
// FILE: Trait_C.java
public interface Trait_C {
    public static String foo() { return "Trait C"; }
}

// FILE: C_K.kt
abstract class C_K : B_K(), Trait_B

// FILE: D_J.java
public abstract class D_J extends C_K {}

// FILE: E_K.kt
abstract class E_K : D_J(), Trait_C

// FILE: F_J.java
public class F_J extends E_K {}

// FILE: main.kt
fun box(): String = F_J.foo()
