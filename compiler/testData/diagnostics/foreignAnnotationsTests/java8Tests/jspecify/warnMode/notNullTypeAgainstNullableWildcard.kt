// FIR_IDENTICAL
// ISSUE: KT-63466
// JSPECIFY_STATE: warn
// FILE: J.java

import org.jspecify.annotations.*;

public class J<T extends @Nullable Object> {
    static J<? extends @Nullable Object> makeJ() {
        return new J<>();
    }

    @NonNull T get() {
        throw new RuntimeException();
    }
}

// FILE: test.kt

fun go(): Any = J.makeJ().get()
