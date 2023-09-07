// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-41215, KT-43551

// FILE: Base.java
public sealed class Base permits A, B {}

// FILE: A.java
public final class A extends Base {}

// FILE: B.java
public sealed class B extends Base permits B.C, B.D {
    public static final class C implements B {}

    public static non-sealed class D extends B {}
}

// FILE: main.kt
fun test_ok_1(base: Base) {
    val x = when (base) {
        is A -> 1
        is B -> 2
    }
}

fun test_ok_2(base: Base) {
    val x = when (base) {
        is A -> 1
        is B.C -> 2
        is B.D -> 3
    }
}

fun test_error_1(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
    }
}

fun test_error_2(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
        is B.C -> 2
    }
}
