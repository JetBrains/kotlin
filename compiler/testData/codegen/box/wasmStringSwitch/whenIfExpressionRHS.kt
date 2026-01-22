fun box(): String {
    val a = true

    return when {
        (if (a) "foo" else "bar") == "" -> "FAIL1"
        (if (a) "foo" else "bar") == "foo" -> "OK"
        else -> "FAIL2"
    }
}
