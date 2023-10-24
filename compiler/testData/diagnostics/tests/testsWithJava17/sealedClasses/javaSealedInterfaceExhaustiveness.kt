// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-41215, KT-43551

// FILE: Base.java
public sealed interface Base permits A, B, E {}

// FILE: A.java
public non-sealed interface A extends Base {}

// FILE: B.java
public sealed interface B extends Base permits B.C, B.D {
    public static final class C implements B {}

    public static non-sealed interface D extends B {}
}

// FILE: E.java
public enum E implements Base {
    First, Second
}

// FILE: SameFile.java
public sealed interface SameFile {
    public static final class A implements SameFile {}
    public static non-sealed class B implements SameFile {}
}

// FILE: main.kt
fun test_ok_1(base: Base) {
    val x = when (base) {
        is A -> 1
        is B -> 2
        is E -> 3
    }
}

fun test_ok_2(base: Base) {
    val x = when (base) {
        is A -> 1
        is B.C -> 2
        is B.D -> 3
        E.First -> 4
        E.Second -> 5
    }
}

fun test_ok_3(sameFile: SameFile) {
    val x = when (sameFile) {
        is SameFile.A -> 1
        is SameFile.B -> 2
    }
}

fun test_error_1(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
        is B -> 2
    }
}

fun test_error_2(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
        is B.C -> 2
        is B.D -> 3
        E.Second -> 5
    }
}

fun test_error_3(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
        is B.C -> 2
        E.First -> 4
        E.Second -> 5
    }
}

fun test_error_4(sameFile: SameFile) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (sameFile) {
        is SameFile.A -> 1
    }
}
