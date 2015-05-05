// !DIAGNOSTICS: -UNUSED_EXPRESSION
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static Boolean staticNN;
    @Nullable
    public static Boolean staticN;
    public static Boolean staticJ;
}

// FILE: k.kt

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    if (platformNN) {}
    if (<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>) {}
    if (platformJ) {}

    while (platformNN) {}
    while (<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>) {}
    while (platformJ) {}

    do {} while (platformNN)
    do {} while (<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>)
    do {} while (platformJ)

    platformNN && false
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!> && false
    platformJ && false

    platformNN || false
    <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!> || false
    platformJ || false

    !platformNN
    !<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>
    !platformJ
}