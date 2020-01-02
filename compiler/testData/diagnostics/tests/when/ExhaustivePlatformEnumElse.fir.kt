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
    return when (J.create()) {
        J.A -> 1
        J.B -> 2
        else -> 0
    }
}
