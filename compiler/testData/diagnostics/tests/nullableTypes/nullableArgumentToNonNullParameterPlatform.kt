// FILE: J.java

import org.jetbrains.annotations.*;

public class J {
    void foo(String x) {}
    void foo(@NotNull Double x) {}
    void foo(@Nullable Byte x) {}
}

// FILE: test.kt

fun test(j: J, nullStr: String?, nullByte: Byte?, nullDouble: Double?) {
    j.foo(nullStr)
    j.foo(<!TYPE_MISMATCH!>nullDouble<!>)
    j.foo(nullByte)
}