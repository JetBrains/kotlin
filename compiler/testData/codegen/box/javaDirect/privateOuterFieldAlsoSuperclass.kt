// TARGET_BACKEND: JVM

// FILE: p/Outer.java
package p;

public class Outer {
    private static final int X = 1;

    public static class Inner extends Outer {
        public static final int Y = X;
    }
}

// FILE: test.kt
const val K = p.Outer.Inner.Y

fun box(): String = if (K == 1) "OK" else "FAIL: $K"
