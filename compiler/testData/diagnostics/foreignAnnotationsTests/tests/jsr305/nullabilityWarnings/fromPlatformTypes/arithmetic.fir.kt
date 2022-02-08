// JSR305_GLOBAL_REPORT: warn

// FILE: J.java
public class J {
    @MyNonnull
    public static Integer staticNN;
    @MyNullable
    public static Integer staticN;
    public static Integer staticJ;
}

// FILE: k.kt
fun test() {
    var platformNN = J.staticNN
    var platformN = J.staticN
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
    1 + platformN
    1 + platformJ

    platformNN + 1
    platformN + 1
    platformJ + 1

    1 <!INFIX_MODIFIER_REQUIRED!>plus<!> platformNN
    1 <!INFIX_MODIFIER_REQUIRED!>plus<!> platformN
    1 <!INFIX_MODIFIER_REQUIRED!>plus<!> platformJ

    platformNN <!INFIX_MODIFIER_REQUIRED!>plus<!> 1
    platformN <!INFIX_MODIFIER_REQUIRED!>plus<!> 1
    platformJ <!INFIX_MODIFIER_REQUIRED!>plus<!> 1

    platformNN += 1
    platformN += 1
    platformJ += 1
}
