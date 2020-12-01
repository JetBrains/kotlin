// ISSUE: KT-13495
// !DIAGNOSTICS: -UNUSED_VARIABLE
// !LANGUAGE: +AllowSealedInheritorsInDifferentFilesOfSamePackage

// FILE: Base.kt

sealed class Base {
    class A : Base()
}

// FILE: B.kt

class B : Base()

// FILE: Container.kt

class Containter {
    class C : Base()

    inner class D : Base()
}

// FILE: main.kt

fun test_OK(base: Base) {
    val x = when (base) {
        is Base.A -> 1
        is B -> 2
        is Containter.C -> 3
        is Containter.D -> 4
    }
}

fun test_error_1(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is B -> 2
        is Containter.C -> 3
        is Containter.D -> 4
    }
}

fun test_error_2(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is Base.A -> 1
        is Containter.C -> 3
        is Containter.D -> 4
    }
}

fun test_error_3(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is Base.A -> 1
        is B -> 2
        is Containter.D -> 4
    }
}

fun test_error_4(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is Base.A -> 1
        is B -> 2
        is Containter.C -> 3
    }
}
