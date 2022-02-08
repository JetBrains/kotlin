fun test_1(cond: Boolean) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (cond) {
        true -> 1
    }

    val y = <!NO_ELSE_IN_WHEN!>when<!> (cond) {
        false -> 2
    }

    val z = when (cond) {
        true -> 1
        false -> 2
    }
}

fun test_2(cond: Boolean?) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (cond) {
        true -> 1
        false -> 2
    }

    val x = when (cond) {
        true -> 1
        false -> 2
        null -> 3
    }
}

fun test_3(cond: Boolean) {
    <!NON_EXHAUSTIVE_WHEN_STATEMENT!>when<!> (cond) {
        true -> 1
    }
}
