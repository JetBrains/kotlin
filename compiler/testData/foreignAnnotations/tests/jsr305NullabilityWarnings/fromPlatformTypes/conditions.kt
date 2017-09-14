// !DIAGNOSTICS: -UNUSED_EXPRESSION
// WARNING_FOR_JSR305_ANNOTATIONS

// FILE: J.java

public class J {
    @MyNonnull
    public static Boolean staticNN;
    @MyNullable
    public static Boolean staticN;
    public static Boolean staticJ;
}

// FILE: k.kt

fun test() {
    val platformNN = J.staticNN
    val platformN = J.staticN
    val platformJ = J.staticJ

    if (platformNN) {}
    if (<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>) {}
    if (platformJ) {}

    while (platformNN) {}
    while (<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>) {}
    while (platformJ) {}

    do {} while (platformNN)
    do {} while (<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS, NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>)
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