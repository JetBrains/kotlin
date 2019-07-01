// WITH_JDK
// FILE: samOperators.kt
fun f() {}

fun J.test1() {
    this[::f]
    this[::f, ::f]
}

fun J.test2() {
    this[::f] = ::f
    this[::f, ::f] = ::f
}

fun J.test3() {
    this += ::f
    this -= ::f
}

// FILE: J.java
public class J {
    public void get(Runnable k) {}
    public void get(Runnable k, Runnable m) {}
    public void set(Runnable k, Runnable v) {}
    public void set(Runnable k, Runnable m, Runnable v) {}
    public void plusAssign(Runnable i) {}
    public void minusAssign(Runnable i) {}
}
