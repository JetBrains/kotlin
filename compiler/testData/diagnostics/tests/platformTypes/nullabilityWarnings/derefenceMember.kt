// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
    public static J staticJ;

    public void foo() {}
}

// FILE: k.kt

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    platformNN.foo()
    platformN<!UNSAFE_CALL!>.<!>foo()
    platformJ.foo()

    with(platformNN) {
        foo()
    }
    with(platformN) {
        <!UNSAFE_CALL!>foo<!>()
    }
    with(platformJ) {
        foo()
    }
}
