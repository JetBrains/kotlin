// !CHECK_TYPE

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

    checkSubtype<J>(platformNN)
    checkSubtype<J>(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>)
    checkSubtype<J>(platformJ)

    checkSubtype<J?>(platformNN)
    checkSubtype<J?>(platformN)
    checkSubtype<J?>(platformJ)
}