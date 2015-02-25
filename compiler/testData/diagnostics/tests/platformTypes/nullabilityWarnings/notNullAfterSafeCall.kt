// FILE: p/J.java

package p;

import org.jetbrains.annotations.*;

public class J {
    public @NotNull String nn() { return ""; }
}

// FILE: k.kt

import p.J

fun test(j: J?) {
    val s = j?.nn()
    if (s != null) {

    }
}