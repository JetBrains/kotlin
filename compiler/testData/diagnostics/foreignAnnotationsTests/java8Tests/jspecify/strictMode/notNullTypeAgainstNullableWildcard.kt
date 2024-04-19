// ISSUE: KT-63466
// JSPECIFY_STATE: strict
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

// jspecify_nullness_mismatch
fun go(): Any = <!TYPE_MISMATCH!>J.makeJ().get()<!>
