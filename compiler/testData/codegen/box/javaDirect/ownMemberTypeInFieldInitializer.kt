// TARGET_BACKEND: JVM
// FILE: p/Outer.java
package p;

public class Outer {
    public static class Defaults {
        public static final int SIZE = 1;
    }
    public static final int DEFAULT_SIZE = Defaults.SIZE;
}

// FILE: test.kt
const val K = p.Outer.DEFAULT_SIZE

fun box(): String = if (K == 1) "OK" else "FAIL: $K"