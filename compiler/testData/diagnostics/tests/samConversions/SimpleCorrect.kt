// !LANGUAGE: +NewInference +SamConversionForKotlinFunctions
// FILE: J.java
public interface J {
    public void foo1(Runnable r);

    public void foo2(Runnable r1, Runnable r2);

    public void foo3(Runnable r1, Runnable r2, Runnable r3);
}

// FILE: Runnable.java
public interface Runnable {
    void run();
}

// FILE: 1.kt
fun test(j: J, r: Runnable) {
    j.foo1(r)
    j.foo1({})

    j.foo2(r, r)
    j.foo2({}, {})
    j.foo2(r, {})
    j.foo2({}, r)

    j.foo3(r, r, r)
    j.foo3(r, r, {})
    j.foo3(r, {}, r)
    j.foo3(r, {}, {})
    j.foo3({}, r, r)
    j.foo3({}, r, {})
    j.foo3({}, {}, r)
    j.foo3({}, {}, {})
}