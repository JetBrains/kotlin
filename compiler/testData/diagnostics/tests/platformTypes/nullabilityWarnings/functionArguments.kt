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
    foo(J.staticNN)
    foo(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.staticN<!>)
    foo(J.staticJ)

    bar(J.staticNN)
    bar(J.staticN)
    bar(J.staticJ)
}

fun foo(j: J) {}
fun bar(j: J?) {}