fun some(a: Int, b: Int) {
    run {
        val i = 12
        val j = 13
        if (a > 50) {
            if (b > 100) {
                101 + j
            } else {
                ~102 * j
            }
        } else {
            return@run false
        }
    }
}

fun <T> run(a: () -> T) {}