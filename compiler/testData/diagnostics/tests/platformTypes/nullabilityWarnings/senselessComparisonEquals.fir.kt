// !DIAGNOSTICS: -UNUSED_EXPRESSION

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

    val a: Any? = null

    if (platformNN != null) {}
    if (null != platformNN) {}
    if (platformNN == null) {}
    if (null == platformNN) {}

    if (a != null && platformNN != a) {}

    if (platformN != null) {}
    if (platformN == null) {}
    if (a == null && platformN == a) {}

    if (platformJ != null) {}
    if (platformJ == null) {}
    if (a == null && platformJ == a) {}
}

