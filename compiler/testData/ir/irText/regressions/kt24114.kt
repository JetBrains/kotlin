// FIR_IDENTICAL
fun one() = 1
fun two() = 2

fun test1(): Int {
    while (true) {
        when (one()) {
            1 -> {
                when(two()) {
                    2 -> return 2
                }
            } // : Nothing
            else -> return 3
        }
    }
}

fun test2(): Int {
    while (true) {
        when (one()) {
            1 ->
                when (two()) {
                    2 -> return 2
                } // : Unit -> Nothing
            else -> return 3
        } // : Nothing
    }
}
