fun box(): String {
    val v = 1

    return when {
        (when (v) {
            1 -> "foo"
            else -> "bar"
        }) == "" -> "FAIL1"
        (when (v) {
            1 -> "foo"
            else -> "bar"
        }) == "foo" -> "OK"
        else -> "FAIL2"
    }
}
