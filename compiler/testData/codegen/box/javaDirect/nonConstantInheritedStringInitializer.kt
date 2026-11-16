// TARGET_BACKEND: JVM
// MODULE: lib
// FILE: q/BinaryBase.java
package q;

public class BinaryBase {
    public static String NAME = null;
}

// MODULE: main(lib)
// FILE: p/J.java
package p;

public class J extends q.BinaryBase {
    public static final String S = NAME;
}

// FILE: test.kt
import p.J

fun box(): String {
    try {
        val s: String = J.S
        return "FAIL: no null check, s == null is ${(s as String?) == null}"
    } catch (e: NullPointerException) {
        return "OK"
    }
}
