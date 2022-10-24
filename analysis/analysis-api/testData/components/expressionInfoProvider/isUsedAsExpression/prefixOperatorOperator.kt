fun test(b: Boolean): Int {
    val (one, two) = b to (<expr>!</expr>b)
    return if (one && two) {
        54
    } else {
        45
    }
}