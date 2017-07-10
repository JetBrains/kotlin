// WITH_RUNTIME
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

// @TestKt.class:
// 2 NEW
// 0 IFNONNULL
// 1 IFNULL
// 1 ACONST_NULL