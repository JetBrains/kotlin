fun test(b: Boolean): Int {
    val (one: <expr>Boolean</expr>, two) = b to !b
    return if (one && two) {
        54
    } else {
        45
    }
}