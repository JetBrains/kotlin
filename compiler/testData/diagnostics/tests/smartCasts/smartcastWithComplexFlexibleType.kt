// FIR_IDENTICAL
// WITH_STDLIB
// ISSUE: KT-68623

// FILE: A.java
public abstract class A<T> {}

// FILE: Some.java
public class Some {
    public A getA() {
        return null;
    }
}

// FILE: main.kt
interface B {
    fun foo()
}

fun test(parent: Some) {
    val a = parent.a ?: return
    if (a !is B) return
    val view = a.takeIf { true }?.foo()
}
