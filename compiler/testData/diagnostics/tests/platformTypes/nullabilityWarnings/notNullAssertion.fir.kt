// !DIAGNOSTICS: -UNUSED_EXPRESSION -SENSELESS_COMPARISON -UNUSED_PARAMETER

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

    if (platformNN != null) {
        platformNN!!
    }

    if (platformN != null) {
        platformN!!
    }

    if (platformJ != null) {
        platformJ!!
    }

    platformNN!!
    platformN!!
    platformJ!!
}
