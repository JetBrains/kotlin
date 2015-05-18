// See KT-6399: exhaustive whens on platform enums

// FILE: J.java

public enum J {
    A, B;

    public static J create() {
        return J.A;
    }
}

// FILE: K.kt

fun foo(): Int {
    // When is not-exhaustive
    return <!NO_ELSE_IN_WHEN!>when<!> (J.create()) {
        J.A -> 1
    }
}