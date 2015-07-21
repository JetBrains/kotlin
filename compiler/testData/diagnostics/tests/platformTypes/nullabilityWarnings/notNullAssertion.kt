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
        platformNN<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }

    if (platformN != null) {
        platformN<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }

    if (platformJ != null) {
        platformJ<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    }

    platformNN<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>
    platformN!!
    platformJ!!
}
