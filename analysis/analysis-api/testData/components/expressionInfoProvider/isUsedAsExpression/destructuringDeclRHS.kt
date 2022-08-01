fun test(b: Boolean): Int {
    val (one, two) = <expr>b to !b</expr>
    return if (one && two) {
        54
    } else {
        45
    }
}