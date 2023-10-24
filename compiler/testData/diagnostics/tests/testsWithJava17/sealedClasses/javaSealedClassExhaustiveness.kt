// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
// ISSUE: KT-41215, KT-43551

// FILE: Base.java
public sealed class Base permits A, B {}

// FILE: A.java
public final class A extends Base {}

// FILE: B.java
public sealed class B extends Base permits B.C, B.D {
    public static final class C extends B {}

    public static non-sealed class D extends B {}
}

// FILE: SameFile.java
public sealed class SameFile {
    public static final class A extends SameFile {}
    public static sealed class B extends SameFile {
        public static final class C extends B {}
        public static non-sealed class D extends B {}
    }
}

// FILE: SameFileNonSealed.java
public class SameFileNonSealed {
    public static final class A extends SameFileNonSealed {}
    public static class B extends SameFileNonSealed {
        public static final class C extends B {}
        public static class D extends B {}
    }
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

fun test_ok_3(sameFile: SameFile) {
    val x = when (sameFile) {
        is SameFile.A -> 1
        is SameFile.B -> 2
    }
}

fun test_ok_4(sameFile: SameFile) {
    val x = when (sameFile) {
        is SameFile.A -> 1
        is SameFile.B.C -> 2
        is SameFile.B.D -> 3
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

fun test_error_3(sameFile: SameFile) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (sameFile) {
        is SameFile.A -> 1
    }
}

fun test_error_4(sameFile: SameFile) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (sameFile) {
        is SameFile.A -> 1
        is SameFile.B.C -> 2
    }
}

fun test_error_5(sameFile: SameFileNonSealed) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (sameFile) {
        is SameFileNonSealed.A -> 1
        is SameFileNonSealed.B -> 2
    }
}

fun test_error_6(sameFile: SameFileNonSealed) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (sameFile) {
        is SameFileNonSealed.A -> 1
        is SameFileNonSealed.B.C -> 2
        is SameFileNonSealed.B.D -> 2
    }
}