// ISSUE: KT-56942
// FILE: Base.java
public sealed class Base permits A, B {
    public static Base provide() { return new A(); }
}

// FILE: A.java
public final class A extends Base {}

// FILE: B.java
public final class B extends Base {}

// FILE: main.kt

fun test_0(base: Base) {
    <!NO_ELSE_IN_WHEN!>when<!> (base) {

    }

    when (base) {
        is A -> {}
        is B -> {}
    }

    when (base) {
        is A -> {}
        is B -> {}
        <!SENSELESS_NULL_IN_WHEN!>null<!> -> {}
    }

    when (base) {
        is A -> {}
        is B -> {}
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> {}
    }
}

fun test_1() {
    <!NO_ELSE_IN_WHEN!>when<!> (Base.provide()) {

    }

    when (Base.provide()) {
        is A -> {}
        is B -> {}
    }

    when (Base.provide()) {
        is A -> {}
        is B -> {}
        null -> {}
    }

    when (Base.provide()) {
        is A -> {}
        is B -> {}
        else -> {}
    }
}
