// FILE: J.java
public class J implements I {
    @Override
    public int foo(int a, int b) {
        return a + b;
    }
}

// FILE: I.kt
interface I {
    fun foo(a: Int, b: Int): Int
}

fun test(j: J) {
    j.foo(1, b = 2)
}