// !LANGUAGE: +NewInference
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
    j.foo(r, <!TYPE_MISMATCH!>{}<!>)
    j.foo(<!TYPE_MISMATCH!>{}<!>, r)
    j.foo(<!TYPE_MISMATCH!>{}<!>, <!TYPE_MISMATCH!>{}<!>)
}