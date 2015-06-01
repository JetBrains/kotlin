// !DIAGNOSTICS: -UNUSED_VARIABLE -SENSELESS_COMPARISON

// FILE: p/J.java
package p;

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static J staticNN;
    @Nullable
    public static J staticN;
    public static J staticJ;
}

// FILE: k.kt

import p.*

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    val v0 = platformNN <!USELESS_ELVIS!>?: J()<!>
    platformNN <!USELESS_ELVIS!>?: J()<!>
    platformN ?: J()
    platformJ ?: J()

    if (platformNN != null) {
        platformNN <!USELESS_ELVIS!>?: J()<!>
    }

    if (platformN != null) {
        platformN <!USELESS_ELVIS!>?: J()<!>
    }

    if (platformJ != null) {
        platformJ <!USELESS_ELVIS!>?: J()<!>
    }
}

