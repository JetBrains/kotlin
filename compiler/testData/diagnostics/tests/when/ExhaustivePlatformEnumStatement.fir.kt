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
    when (J.create()) {
        J.A -> return 1
        J.B -> return 2
    }
}
