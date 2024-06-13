// LANGUAGE: -SamConversionForKotlinFunctions
// FILE: Runnable.java
public interface Runnable {
    void run();
}

// FILE: 1.kt
interface K {
    fun foo1(r: Runnable)
    fun foo2(r1: Runnable, r2: Runnable)
}
fun test(k: K, r: Runnable) {
    k.foo1(r)
    k.foo1({})

    k.foo2(r, r)
    k.foo2({}, {})
    k.foo2(r, {})
    k.foo2({}, r)
}