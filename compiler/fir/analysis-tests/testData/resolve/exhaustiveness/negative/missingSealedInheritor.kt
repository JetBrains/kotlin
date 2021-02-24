// FILE: a.kt

sealed class Base

class A : Base

// FILE: b.kt

object B : Base()

// FILE: c.kt

fun test_1(base: Base) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
    }

    val y = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        B -> 1
    }

    val z = when (base) {
        is A -> 1
        B -> 2
    }
}

fun test_2(base: Base?) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
        is B -> 2
    }

    val y = <!NO_ELSE_IN_WHEN!>when<!> (base) {
        is A -> 1
        B -> 2
    }

    val z = when (base) {
        is A -> 1
        B -> 2
        null -> 3
    }
}

fun test_3(base: Base) {
    when (base) {
        is A -> 1
    }
}
