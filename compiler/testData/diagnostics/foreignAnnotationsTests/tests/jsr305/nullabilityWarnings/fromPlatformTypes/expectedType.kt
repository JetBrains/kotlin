// FIR_IDENTICAL
// !CHECK_TYPE
// JSR305_GLOBAL_REPORT: warn

// FILE: J.java
public class J {
    @MyNonnull
    public static J staticNN;
    @MyNullable
    public static J staticN;
    public static J staticJ;
}

// FILE: k.kt
fun test() {
    val platformNN = J.staticNN
    val platformN = J.staticN
    val platformJ = J.staticJ

    checkSubtype<J>(platformNN)
    checkSubtype<J>(<!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>)
    checkSubtype<J>(platformJ)

    checkSubtype<J?>(platformNN)
    checkSubtype<J?>(platformN)
    checkSubtype<J?>(platformJ)
}
