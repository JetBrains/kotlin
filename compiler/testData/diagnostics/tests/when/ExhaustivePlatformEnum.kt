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
    // When is exhaustive (count a platform enum as a special case)
    return when (<!WHEN_ENUM_CAN_BE_NULL_IN_JAVA!>J.create()<!>) {
        J.A -> 1
        J.B -> 2
    }
}
