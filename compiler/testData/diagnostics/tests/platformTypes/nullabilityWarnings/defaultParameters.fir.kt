// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
    public static J staticJ;
}

// FILE: k.kt

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    fun foo(p: J = platformNN, p1: J = <!INITIALIZER_TYPE_MISMATCH!>platformN<!>, p2: J = platformJ) {}

    fun foo1(p: J? = platformNN, p1: J? = platformN, p2: J? = platformJ) {}
}