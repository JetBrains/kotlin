fun test(b: Boolean): Int {
    val (<expr>one</expr>, two) = b to !b
    return if (one && two) {
        54
    } else {
        45
    }
}