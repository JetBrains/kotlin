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
    if (<!TYPE_MISMATCH, TYPE_MISMATCH_IN_CONDITION!>platformN<!>) {}
    if (platformJ) {}

    while (platformNN) {}
    while (<!TYPE_MISMATCH, TYPE_MISMATCH_IN_CONDITION!>platformN<!>) {}
    while (platformJ) {}

    do {} while (platformNN)
    do {} while (<!TYPE_MISMATCH, TYPE_MISMATCH_IN_CONDITION!>platformN<!>)
    do {} while (platformJ)

    platformNN && false
    <!TYPE_MISMATCH!>platformN<!> && false
    platformJ && false

    platformNN || false
    <!TYPE_MISMATCH!>platformN<!> || false
    platformJ || false

    !platformNN
    <!UNSAFE_CALL!>!<!>platformN
    !platformJ
}