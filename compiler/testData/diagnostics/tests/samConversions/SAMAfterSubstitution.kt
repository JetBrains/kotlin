// FIR_IDENTICAL
// FILE: J.java
public interface J<T> {
    public void foo(T r1, T r2);
}

// FILE: Runnable.java
public interface Runnable {
    void run();
}

// FILE: 1.kt
fun test(j: J<Runnable>, r: Runnable) {
    j.foo(r, r)
    j.foo(r, {})
    j.foo({}, r)
    j.foo({}, {})
}