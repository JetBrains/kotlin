// FILE: JFoo.java

import org.jetbrains.annotations.Nullable;

public class JFoo {
    public static void foo2(@Nullable Runnable h1, @Nullable Runnable h2) {
        if (h2 != null) throw new AssertionError();
        h1.run();
    }
}

// FILE: Test.kt
fun test() {
    var i = 0
    JFoo.foo2({ i++ }, null)
}

// @TestKt.class:
// 1 NEW TestKt\$
// 1 NEW kotlin/jvm/internal/Ref\$IntRef
// 2 NEW
// 0 IFNONNULL
// 0 IFNULL
// 1 ACONST_NULL
