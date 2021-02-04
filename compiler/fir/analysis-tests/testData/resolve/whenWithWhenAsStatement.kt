fun test(value: Int) {
    when (value) {
        0 -> {}
        1 -> when (value) {
            2 -> false
        }
    }
}
