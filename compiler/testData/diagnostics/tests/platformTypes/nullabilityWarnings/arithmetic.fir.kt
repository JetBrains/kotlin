// !WITH_NEW_INFERENCE
// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static Integer staticNN;
    @Nullable
    public static Integer staticN;
    public static Integer staticJ;
}

// FILE: k.kt

fun test() {
    // @NotNull platform type
    var platformNN = J.staticNN
    // @Nullable platform type
    var platformN = J.staticN
    // platform type with no annotation
    var platformJ = J.staticJ

    +platformNN
    <!INAPPLICABLE_CANDIDATE!>+<!>platformN
    +platformJ

    ++platformNN
    <!INAPPLICABLE_CANDIDATE!>++<!>platformN
    ++platformJ

    platformNN++
    platformN<!INAPPLICABLE_CANDIDATE!>++<!>
    platformJ++

    1 + platformNN
    1 <!INAPPLICABLE_CANDIDATE!>+<!> platformN
    1 + platformJ

    platformNN + 1
    platformN <!INAPPLICABLE_CANDIDATE!>+<!> 1
    platformJ + 1

    1 plus platformNN
    1 <!INAPPLICABLE_CANDIDATE!>plus<!> platformN
    1 plus platformJ

    platformNN plus 1
    platformN <!INAPPLICABLE_CANDIDATE!>plus<!> 1
    platformJ plus 1

    platformNN += 1
    <!UNRESOLVED_REFERENCE!>platformN += 1<!>
    platformJ += 1
}