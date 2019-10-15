fun test_1(b: Boolean) {
    val x = when (b) {
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

fun test_2(b: Boolean?) {
    val x = when (b) {
        true -> 1
        false -> 2
    }
    val y = when (b) {
        true -> 1
        false -> 2
        null -> 3
    }
}