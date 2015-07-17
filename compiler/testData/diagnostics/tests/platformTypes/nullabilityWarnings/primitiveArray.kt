// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static int[] staticNN;
    @Nullable
    public static int[] staticN;
    public static int[] staticJ;
}

// FILE: k.kt

fun test() {
    // @NotNull platform type
    val platformNN = J.staticNN
    // @Nullable platform type
    val platformN = J.staticN
    // platform type with no annotation
    val platformJ = J.staticJ

    platformNN[0]
    <!UNSAFE_CALL!>platformN[0]<!>
    platformJ[0]

    platformNN[0] = 1
    <!UNSAFE_CALL!>platformN[0]<!>  = 1
    platformJ[0]  = 1
}

