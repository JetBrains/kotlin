// !DIAGNOSTICS: -UNUSED_PARAMETER
// FILE: p/J.java
package p;

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
    public static J staticJ;
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

    fun foo(p: J = platformNN, p1: J = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>, p2: J = platformJ) {}

    fun foo1(p: J? = platformNN, p1: J? = platformN, p2: J? = platformJ) {}
}