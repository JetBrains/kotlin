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
    <!UNSAFE_CALL!>+<!>platformN
    +platformJ

    ++platformNN
    <!UNSAFE_CALL!>++<!>platformN
    ++platformJ

    platformNN++
    platformN<!UNSAFE_CALL!>++<!>
    platformJ++

    1 + platformNN
    1 <!NONE_APPLICABLE!>+<!> platformN
    1 + platformJ

    platformNN + 1
    platformN <!UNSAFE_INFIX_CALL!>+<!> 1
    platformJ + 1

    1 plus platformNN
    1 <!NONE_APPLICABLE!>plus<!> platformN
    1 plus platformJ

    platformNN plus 1
    platformN <!UNSAFE_INFIX_CALL!>plus<!> 1
    platformJ plus 1

    platformNN += 1
    platformN <!UNSAFE_INFIX_CALL!>+=<!> 1
    platformJ += 1
}

