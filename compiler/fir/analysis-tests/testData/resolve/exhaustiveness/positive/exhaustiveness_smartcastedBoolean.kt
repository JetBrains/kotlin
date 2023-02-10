// WITH_STDLIB
// ISSUE: KT-55379

fun test_1(b: Any) {
    require(b is Boolean)
    val x = <!NO_ELSE_IN_WHEN!>when<!> (b) {
        true -> 1
    }
    val y = when (b) {
        true -> 1
        false -> 2
    }
    val z = when (b) {
        true -> 1
        else -> 2
    }
}

fun test_2(b: Any?) {
    require(b is Boolean?)
    val x = <!NO_ELSE_IN_WHEN!>when<!> (b) {
        true -> 1
        false -> 2
    }
    val y = when (b) {
        true -> 1
        false -> 2
        null -> 3
    }
}
