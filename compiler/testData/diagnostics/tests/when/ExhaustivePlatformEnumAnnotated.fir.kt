// See KT-6399: exhaustive whens on platform enums

// FILE: J.java

import org.jetbrains.annotations.*;

public enum J {
    A, B;

    @NotNull public static J create() {
        return J.A;
    }
}

// FILE: K.kt

fun foo(): Int {
    // When is exhaustive (count a platform enum as a special case)
    return when (J.create()) {
        J.A -> 1
        J.B -> 2
    }
}
