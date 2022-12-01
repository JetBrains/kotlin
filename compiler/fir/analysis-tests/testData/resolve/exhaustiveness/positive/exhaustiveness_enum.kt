enum class Enum {
    A, B, C
}

fun test_1(e: Enum) {
    val a = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        Enum.A -> 1
        Enum.B -> 2
    }

    val b = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        Enum.A -> 1
        Enum.B -> 2
        is String -> 3
    }

    val c = when (e) {
        Enum.A -> 1
        Enum.B -> 2
        Enum.C -> 3
    }

    val d = when (e) {
        Enum.A -> 1
        else -> 2
    }
}

fun test_2(e: Enum?) {
    val a = <!NO_ELSE_IN_WHEN!>when<!> (e) {
        Enum.A -> 1
        Enum.B -> 2
        Enum.C -> 3
    }

    val b = when (e) {
        Enum.A -> 1
        Enum.B -> 2
        Enum.C -> 3
        null -> 4
    }

    val c = when (e) {
        Enum.A -> 1
        Enum.B -> 2
        Enum.C -> 3
        else -> 4
    }
}

fun test_3(e: Enum) {
    val a = when (e) {
        Enum.A, Enum.B -> 1
        Enum.C -> 2
    }
}
