// !DIAGNOSTICS: -UNUSED_EXPRESSION
// JSR305_GLOBAL_REPORT: warn

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
    if (<!CONDITION_TYPE_MISMATCH!>platformN<!>) {}
    if (platformJ) {}

    while (platformNN) {}
    while (<!CONDITION_TYPE_MISMATCH!>platformN<!>) {}
    while (platformJ) {}

    do {} while (platformNN)
    do {} while (<!CONDITION_TYPE_MISMATCH!>platformN<!>)
    do {} while (platformJ)

    platformNN && false
    <!CONDITION_TYPE_MISMATCH!>platformN<!> && false
    platformJ && false

    platformNN || false
    <!CONDITION_TYPE_MISMATCH!>platformN<!> || false
    platformJ || false

    !platformNN
    <!UNSAFE_CALL!>!<!>platformN
    !platformJ
}
