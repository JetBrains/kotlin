// TARGET_BACKEND: JVM
// FILE: a.kt
import p.J

fun early(): String = J.S

// FILE: p/J.java
package p;

public class J {
    public static final String S = q.Consts.Y;
}

// FILE: q/b.kt
package q

object Consts {
    const val Y: String = "a" + "b"
}

// FILE: c.kt
import p.J

const val K = J.S

fun box(): String = if (K == "ab" && early() == "ab") "OK" else "FAIL: $K"
