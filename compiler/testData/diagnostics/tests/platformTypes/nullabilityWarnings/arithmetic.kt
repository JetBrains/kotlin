// FILE: p/J.java
package p;

import org.jetbrains.annotations.*;

public class J {
    @NotNull
    public static Integer staticNN;
    @Nullable
    public static Integer staticN;
    public static Integer staticJ;
}

// FILE: k.kt

import p.*

fun test() {
    // @NotNull platform type
    var platformNN = J.staticNN
    // @Nullable platform type
    var platformN = J.staticN
    // platform type with no annotation
    var platformJ = J.staticJ

    +platformNN
    +platformN
    +platformJ

    ++platformNN
    ++platformN
    ++platformJ

    platformNN++
    platformN++
    platformJ++

    1 + platformNN
    1 + <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>
    1 + platformJ

    platformNN + 1
    platformN + 1
    platformJ + 1

    1 plus platformNN
    1 plus <!NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS!>platformN<!>
    1 plus platformJ

    platformNN plus 1
    platformN plus 1
    platformJ plus 1

    platformNN += 1
    platformN += 1
    platformJ += 1
}

