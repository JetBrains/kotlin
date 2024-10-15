// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-67998
// WITH_STDLIB

// FILE: J.java
public class J {
    public interface F<XJ> {
        XJ foo();
    }

    public static void run(F<?> f) {
        f.foo();
    }
}

// FILE: k.kt
class K {
    fun interface F<XK> {
        fun foo(): XK
    }

    companion object {
        fun run(f: F<*>) {
            f.foo()
        }
    }
}

fun test(p: Boolean) {
    J.run { if (p) listOf("") else emptyList() }
    K.run { if (p) listOf("") else emptyList() }
}
