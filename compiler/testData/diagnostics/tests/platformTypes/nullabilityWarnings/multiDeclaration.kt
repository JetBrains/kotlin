// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: p/J.java
package p;

import org.jetbrains.annotations.*;

public class J {
    public interface Multi {
        String component1();
        String component2();
    }

    @NotNull
    public static Multi staticNN;
    @Nullable
    public static Multi staticN;
    public static Multi staticJ;
}

// FILE: k.kt

import p.*

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    val (a1, b1) = platformNN
    val (a2, b2) = platformN
    val (a3, b3) = platformJ
}

