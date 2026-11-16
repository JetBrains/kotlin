// TARGET_BACKEND: JVM
// FILE: q/Holder.java
package q;

public class Holder {
    public static String MAYBE_NULL = null;
}

// FILE: p/J.java
package p;

import static q.Holder.*;

public class J {
    public static final String S = MAYBE_NULL;
}

// FILE: test.kt
import p.J

fun box(): String {
    try {
        val s: String = J.S
        return if ((s as String?) == null) "OK" else "FAIL: expected null"
    } catch (e: NullPointerException) {
        return "OK"
    }
}
