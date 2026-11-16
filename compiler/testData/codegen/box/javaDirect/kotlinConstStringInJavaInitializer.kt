// TARGET_BACKEND: JVM
// FILE: a.kt
import p.J

const val K = J.S

fun box(): String = if (K == "ab") "OK" else "FAIL: $K"

// FILE: p/J.java
package p;

public class J {
    public static final String S = q.Consts.Y;
}

// FILE: q/b.kt
package q

object Consts {
    const val Y = "a" + "b"
}
