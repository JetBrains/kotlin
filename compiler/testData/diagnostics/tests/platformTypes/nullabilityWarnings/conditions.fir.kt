// DIAGNOSTICS: -UNUSED_EXPRESSION
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