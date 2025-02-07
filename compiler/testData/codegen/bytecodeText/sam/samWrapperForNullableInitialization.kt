// SAM_CONVERSIONS: INDY
// FILE: JFoo.java

import org.jetbrains.annotations.Nullable;

public class JFoo {
    public static void foo2(@Nullable Runnable h1, @Nullable Runnable h2) {
        if (h2 != null) throw new AssertionError();
        h1.run();
    }
}

// FILE: Test.kt
fun runnable(): (() -> Unit)? = null

fun test() {
    JFoo.foo2({}, runnable())
}

// @TestKt.class
// 0 NEW
// 0 IFNONNULL
// 1 IFNULL
// 2 ACONST_NULL
