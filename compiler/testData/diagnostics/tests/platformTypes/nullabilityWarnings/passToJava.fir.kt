// !WITH_NEW_INFERENCE
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
    public static J staticJ;

    public static void staticSet(@NotNull J nn, @Nullable J n, J j) {}

    public J(@NotNull J nn, @Nullable J n, J j) {}
    public J() {}

    @NotNull
    public J nn;
    @Nullable
    public J n;
    public J j;

    public void set(@NotNull J nn, @Nullable J n, J j) {}
}

// FILE: k.kt

fun test(n: J?, nn: J) {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    J.staticNN = n
    J.staticNN = platformN
    J.staticNN = nn
    J.staticNN = platformNN
    J.staticNN = platformJ

    J.staticN = n
    J.staticN = platformN
    J.staticN = nn
    J.staticN = platformNN
    J.staticN = platformJ

    J.staticJ = n
    J.staticJ = platformN
    J.staticJ = nn
    J.staticJ = platformNN
    J.staticJ = platformJ

    J.staticSet(nn, nn, nn)
    J.staticSet(platformNN, platformNN, platformNN)
    J.<!INAPPLICABLE_CANDIDATE!>staticSet<!>(n, n, n)
    J.<!INAPPLICABLE_CANDIDATE!>staticSet<!>(platformN, platformN, platformN)
    J.staticSet(platformJ, platformJ, platformJ)

    J().nn = n
    J().nn = platformN
    J().nn = nn
    J().nn = platformNN
    J().nn = platformJ

    J().n = n
    J().n = platformN
    J().n = nn
    J().n = platformNN
    J().n = platformJ

    J().j = n
    J().j = platformN
    J().j = nn
    J().j = platformNN
    J().j = platformJ

    J().set(nn, nn, nn)
    J().set(platformNN, platformNN, platformNN)
    J().<!INAPPLICABLE_CANDIDATE!>set<!>(n, n, n)
    J().<!INAPPLICABLE_CANDIDATE!>set<!>(platformN, platformN, platformN)
    J().set(platformJ, platformJ, platformJ)

    J(nn, nn, nn)
    J(platformNN, platformNN, platformNN)
    <!INAPPLICABLE_CANDIDATE!>J<!>(n, n, n)
    <!INAPPLICABLE_CANDIDATE!>J<!>(platformN, platformN, platformN)
    J(platformJ, platformJ, platformJ)
}