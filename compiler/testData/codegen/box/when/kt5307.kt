fun box(): String {
    val value = 1
    when (value) {
        0 -> {}
        1 -> when (value) {
            2 -> false
        }
    }

    return "OK"
}