// !DIAGNOSTICS: -UNUSED_EXPRESSION

// FILE: p/J.java

package p;

public class J {
    public String s() { return null; }
}

// FILE: k.kt
import p.*

fun test(j: J) {
    j.s()?.length ?: ""
}
