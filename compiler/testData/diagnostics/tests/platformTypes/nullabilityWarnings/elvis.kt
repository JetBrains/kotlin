// !DIAGNOSTICS: -UNUSED_VARIABLE -SENSELESS_COMPARISON

// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
    public static J staticJ;
}

// FILE: k.kt

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    val v0 = platformNN <!USELESS_ELVIS!>?:<!> J()
    platformNN <!USELESS_ELVIS!>?:<!> J()
    platformN ?: J()
    platformJ ?: J()

    if (platformNN != null) {
        <!USELESS_ELVIS!>platformNN<!> ?: J()
    }

    if (platformN != null) {
        <!USELESS_ELVIS!>platformN<!> ?: J()
    }

    if (platformJ != null) {
        <!USELESS_ELVIS!>platformJ<!> ?: J()
    }
}

