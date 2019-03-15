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
    <!UNSAFE_CALL!>+<!>platformN
    +platformJ

    ++platformNN
    <!UNSAFE_CALL!>++<!>platformN
    ++platformJ

    platformNN++
    platformN<!UNSAFE_CALL!>++<!>
    platformJ++

    1 + platformNN
    1 <!NI;NONE_APPLICABLE!>+<!> <!OI;TYPE_MISMATCH!>platformN<!>
    1 + platformJ

    platformNN + 1
    platformN <!UNSAFE_OPERATOR_CALL!>+<!> 1
    platformJ + 1

    1 <!INFIX_MODIFIER_REQUIRED!>plus<!> platformNN
    1 <!NI;NONE_APPLICABLE, OI;INFIX_MODIFIER_REQUIRED!>plus<!> <!OI;TYPE_MISMATCH!>platformN<!>
    1 <!INFIX_MODIFIER_REQUIRED!>plus<!> platformJ

    platformNN <!INFIX_MODIFIER_REQUIRED!>plus<!> 1
    platformN <!INFIX_MODIFIER_REQUIRED, UNSAFE_INFIX_CALL!>plus<!> 1
    platformJ <!INFIX_MODIFIER_REQUIRED!>plus<!> 1

    platformNN += 1
    platformN <!UNSAFE_OPERATOR_CALL!>+=<!> 1
    platformJ += 1
}