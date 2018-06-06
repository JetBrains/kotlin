// !DIAGNOSTICS: -UNUSED_PARAMETER
// JSR305_GLOBAL_REPORT warn

// FILE: J.java

public class J {
    @MyNullable
    public static J staticN;
}

// FILE: JJ.java

public class JJ {
    public static JJ staticNN;
}

// FILE: JJJ.java

public class JJJ {
    @MyNonnull
    public static JJJ staticNNN;
}

// FILE: k.kt

fun test() {
    val a = J.staticN ?: null
    foo(a)
    val b = JJ.staticNN ?: null
    foo(b)
    val c = JJJ.staticNNN ?: null
    foo(c)
}

fun foo(a: Any?) {
}
