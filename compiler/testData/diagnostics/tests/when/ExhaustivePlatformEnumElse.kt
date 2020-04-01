// FIR_IDENTICAL
/*
 * KOTLIN DIAGNOSTICS SPEC TEST (POSITIVE)
 *
 * SPEC VERSION: 0.1-313
 * PRIMARY LINKS: expressions, when-expression -> paragraph 5 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 1
 * expressions, when-expression, exhaustive-when-expressions -> paragraph 2 -> sentence 9
 */

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
