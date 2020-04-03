// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
}

// FILE: k.kt

fun test() {
    val n = J.staticN
    <!INAPPLICABLE_CANDIDATE!>foo<!>(n)
    J.staticNN = n
    if (n != null) {
        foo(n)
        J.staticNN = n
    }

    val x: J? = null
    J.staticNN = x
    if (x != null) {
        J.staticNN = x
    }
}

fun foo(j: J) {}