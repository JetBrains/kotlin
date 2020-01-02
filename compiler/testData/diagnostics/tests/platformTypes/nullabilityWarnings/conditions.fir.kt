// !WITH_NEW_INFERENCE
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
    if (platformN) {}
    if (platformJ) {}

    while (platformNN) {}
    while (platformN) {}
    while (platformJ) {}

    do {} while (platformNN)
    do {} while (platformN)
    do {} while (platformJ)

    platformNN && false
    platformN && false
    platformJ && false

    platformNN || false
    platformN || false
    platformJ || false

    !platformNN
    <!INAPPLICABLE_CANDIDATE!>!<!>platformN
    !platformJ
}