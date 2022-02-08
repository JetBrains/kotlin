// FILE: J.java
import org.jetbrains.annotations.*;
import java.util.List;

class J {
    static String foo() { return "abc"; }

    @NotNull
    static List<String> bar() { return new List<String>(); }
}

// FILE: test.kt

fun bar() {
    var v: String?
    v = J.foo()
    <!DEBUG_INFO_SMARTCAST!>v<!>.length
    gav(<!DEBUG_INFO_SMARTCAST!>v<!>)

    var l: List<String>?
    l = J.bar()
    <!DEBUG_INFO_SMARTCAST!>l<!>.isEmpty()
}

fun gav(v: String) = v
