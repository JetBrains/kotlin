fun test(b: Boolean): Int {
    !<expr>b</expr>
    val (one, two) = b to !b
    return if (one && two) {
        54
    } else {
        45
    }
}