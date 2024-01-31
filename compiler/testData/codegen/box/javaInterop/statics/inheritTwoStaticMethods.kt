// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// FILE: A.java
public interface A {
    static String foo() { return "OK"; }
}

// FILE: B.java
public interface B {
    static String foo() { return "Fail"; }
}

// FILE: box.kt
class C : A, B {
    fun test(): String = A.foo()
}

fun box(): String = C().test()
