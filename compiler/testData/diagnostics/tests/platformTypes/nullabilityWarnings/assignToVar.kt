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

var v: J = J()
var n: J? = J()

fun test() {
    v = J.staticNN
    v = <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.staticN<!>
    v = J.staticJ

    n = J.staticNN
    n = J.staticN
    n = J.staticJ
}