// FILE: p/J.java
package p;

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static Exception staticNN;
    @Nullable
    public static Exception staticN;
    public static Exception staticJ;
}

// FILE: k.kt

import p.*

fun test() {
    throw J.staticNN
}

fun test1() {
    throw <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>J.staticN<!>
}

fun test2() {
    throw J.staticJ
}