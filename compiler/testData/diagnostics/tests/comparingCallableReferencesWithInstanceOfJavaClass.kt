// FIR_IDENTICAL
// ISSUE: KT-13451

// FILE: J.java

public class J {
}

// FILE: Main.kt

class K {
    fun f() {}
}

fun test (j: J, k: K) {
    j == K::f
    j == k::f

    when (j) {
        k::f -> ""
        K::f -> ""
    }
}