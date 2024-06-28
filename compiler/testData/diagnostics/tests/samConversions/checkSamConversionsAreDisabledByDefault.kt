// LANGUAGE: -SamConversionForKotlinFunctions
// FILE: Runnable.java
public interface Runnable {
    void run();
}

// FILE: 1.kt

interface K {
    fun foo1(r: Runnable)
    fun foo2(r1: Runnable, r2: Runnable)
    fun foo3(r1: Runnable, r2: Runnable, r3: Runnable)
}
fun test(k: K, r: Runnable) {
    k.foo1(r)
    k.foo1(<!TYPE_MISMATCH!>{}<!>)

    k.foo2(r, r)
    k.foo2(<!TYPE_MISMATCH!>{}<!>, <!TYPE_MISMATCH!>{}<!>)
    k.foo2(r, <!TYPE_MISMATCH!>{}<!>)
    k.foo2(<!TYPE_MISMATCH!>{}<!>, r)

    k.foo3(r, r, r)
    k.foo3(r, r, <!TYPE_MISMATCH!>{}<!>)
    k.foo3(r, <!TYPE_MISMATCH!>{}<!>, r)
    k.foo3(r, <!TYPE_MISMATCH!>{}<!>, <!TYPE_MISMATCH!>{}<!>)
    k.foo3(<!TYPE_MISMATCH!>{}<!>, r, r)
    k.foo3(<!TYPE_MISMATCH!>{}<!>, r, <!TYPE_MISMATCH!>{}<!>)
    k.foo3(<!TYPE_MISMATCH!>{}<!>, <!TYPE_MISMATCH!>{}<!>, r)
    k.foo3(<!TYPE_MISMATCH!>{}<!>, <!TYPE_MISMATCH!>{}<!>, <!TYPE_MISMATCH!>{}<!>)
}