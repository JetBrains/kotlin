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
    v.length
    gav(v)

    var l: List<String>?
    l = J.bar()
    l.isEmpty()
}

fun gav(v: String) = v
