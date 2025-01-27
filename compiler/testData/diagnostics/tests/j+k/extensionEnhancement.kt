// FIR_IDENTICAL
// ISSUE: KT-74728
// RUN_PIPELINE_TILL: BACKEND
// FILE: J.java
public class J implements K {
    @Override
    public void foo(String p1, String... p2) {}
}

// FILE: K.kt
interface K {
    fun String.foo(vararg s: String)
}

fun test(j: J) {
    with(j) {
        "".foo("1", "2")
    }
}