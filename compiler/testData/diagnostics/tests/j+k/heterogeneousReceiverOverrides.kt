// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// FILE: J.java
public class J implements K1, K2 {
    @Override
    public void foo(String p1) {}
}

// FILE: K.kt
interface K1 {
    fun String.foo()
}

interface K2 {
    fun foo(s: String)
}

fun test(j: J) {
    with(j) {
        "".foo()
    }
}